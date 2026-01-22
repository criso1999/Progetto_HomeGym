package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@WebServlet({"/staff/plans/download", "/client/plans/download"})
public class PlanDownloadServlet extends HttpServlet {

    private TrainingPlanDAO planDao;
    private UtenteDAO utenteDao;
    // base directory dove TrainingPlanActionServlet salva i file (UPLOAD_DIR/training_plans)
    private Path uploadsBase;   // uploadDir/training_plans
    private Path uploadDirPath; // uploadDir

    @Override
    public void init() throws ServletException {
        try {
            planDao = new TrainingPlanDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare DAO", e);
        }
        String uploadDir = System.getenv().getOrDefault("UPLOAD_DIR", "/tmp/homegym_uploads");
        uploadsBase = Paths.get(uploadDir, "training_plans");
        try {
            uploadDirPath = Paths.get(uploadDir).toRealPath(); // may throw, OK
        } catch (IOException e) {
            // fallback: use non-real path but still continue
            uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        }
    }

    private HttpSession requireSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return s;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = requireSession(req, resp);
        if (session == null) return;

        // cast alla tua classe Utente presente nel progetto
        Utente user = (Utente) session.getAttribute("user");

        String planIdParam = req.getParameter("id");            // staff link uses id=...
        String planIdParam2 = req.getParameter("planId");      // client link uses planId=...
        String assignmentIdParam = req.getParameter("assignmentId"); // optional (client)
        String pathParam = req.getParameter("path");           // optional direct path (not recommended)

        Integer planId = null;
        if (planIdParam != null && !planIdParam.isBlank()) planId = parseInt(planIdParam);
        if (planId == null && planIdParam2 != null && !planIdParam2.isBlank()) planId = parseInt(planIdParam2);

        try {
            TrainingPlan plan = null;
            if (planId != null) plan = planDao.findById(planId);

            // if no plan and no path -> not found
            if (plan == null && (pathParam == null || pathParam.isBlank())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Scheda non trovata.");
                return;
            }

            // Authorization:
            boolean allowed = false;
            if (user != null) {
                String ruolo = user.getRuolo();
                if ("PROPRIETARIO".equals(ruolo)) {
                    allowed = true;
                } else if ("PERSONALE".equals(ruolo)) {
                    // staff can download if they created the plan
                    if (plan != null && plan.getCreatedBy() != null && plan.getCreatedBy().equals(user.getId())) allowed = true;
                } else if ("CLIENTE".equals(ruolo)) {
                    // client: check assignment (planDao.listAssignmentsForUser)
                    List<TrainingPlanAssignment> assigns = planDao.listAssignmentsForUser(user.getId());
                    for (TrainingPlanAssignment a : assigns) {
                        if (plan != null && a.getPlanId() == plan.getId()) {
                            allowed = true;
                            break;
                        }
                        if (assignmentIdParam != null && !assignmentIdParam.isBlank()) {
                            Integer assignId = parseInt(assignmentIdParam);
                            if (assignId != null && a.getId() == assignId) { allowed = true; break; }
                        }
                    }
                }
            }

            if (!allowed) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Non autorizzato a scaricare questo file.");
                return;
            }

            // determine file path
            Path fileOnDisk = null;
            String filename = null;
            String contentType = null;

            String uploadDir = uploadDirPath.toString();

            if (plan != null && plan.getAttachmentPath() != null && !plan.getAttachmentPath().isBlank()) {
                String stored = plan.getAttachmentPath(); // e.g. "/uploads/training_plans/123/160000_file.pdf" o "uploads/..."
                String relative = stored.replaceFirst("^/+", ""); // rimuovi leading /
                if (relative.startsWith("uploads/")) {
                    String tail = relative.substring("uploads/".length()); // e.g. "training_plans/123/..."
                    fileOnDisk = Paths.get(uploadDir).resolve(tail).normalize();
                } else {
                    // se il path salvato è già relativo a uploadDir o assoluto
                    Path p = Paths.get(relative);
                    if (p.isAbsolute()) fileOnDisk = p.normalize();
                    else fileOnDisk = uploadsBase.resolve(relative).normalize();
                }
                filename = Optional.ofNullable(plan.getAttachmentFilename()).orElse(fileOnDisk.getFileName().toString());
                contentType = plan.getAttachmentContentType();
            } else if (pathParam != null && !pathParam.isBlank()) {
                // accept only filename to avoid traversal
                String baseName = Paths.get(pathParam).getFileName().toString();
                fileOnDisk = uploadsBase.resolve(baseName).normalize();
                filename = baseName;
            }

            // safety: fileOnDisk must exist and be under uploadDirPath
            if (fileOnDisk == null || !Files.exists(fileOnDisk)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato.");
                return;
            }
            Path fileReal;
            try {
                fileReal = fileOnDisk.toRealPath();
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato.");
                return;
            }

            if (!fileReal.startsWith(uploadDirPath)) {
                // sicurezza: non serviamo file al di fuori della cartella upload
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso al file non permesso.");
                return;
            }

            // content type
            if (contentType == null || contentType.isBlank()) {
                try { contentType = Files.probeContentType(fileReal); } catch (IOException ignored) {}
            }
            if (contentType == null) contentType = "application/octet-stream";

            // stream file
            resp.setContentType(contentType);
            resp.setHeader("Content-Length", String.valueOf(Files.size(fileReal)));
            // use RFC5987 filename* for safer UTF-8 filenames and fallback filename
            String encoded = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            String disposition = "attachment; filename=\"" + filename.replace("\"", "_") + "\"; filename*=UTF-8''" + encoded;
            resp.setHeader("Content-Disposition", disposition);

            try (InputStream in = Files.newInputStream(fileReal);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                out.flush();
            }

        } catch (SQLException sqle) {
            throw new ServletException("Errore DB nel download", sqle);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
}
