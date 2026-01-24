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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@WebServlet({"/staff/plans/download", "/client/plans/download"})
public class PlanDownloadServlet extends HttpServlet {

    private TrainingPlanDAO planDao;
    private UtenteDAO utenteDao;
    // base directory dove TrainingPlanActionServlet salva i file (UPLOAD_DIR)
    private Path uploadsBase;   // e.g. /uploads/training_plans
    private Path uploadDirPath; // e.g. /uploads

    @Override
    public void init() throws ServletException {
        try {
            planDao = new TrainingPlanDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare DAO", e);
        }

        String uploadDir = System.getenv().getOrDefault("UPLOAD_DIR", "/tmp/homegym_uploads");
        // non usare toRealPath() all'inizio: se la mount non esiste ancora fallisce
        uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        uploadsBase = uploadDirPath.resolve("training_plans").toAbsolutePath().normalize();

        // (opzionale) se vuoi che la servlet crei la directory se manca:
        try {
            Files.createDirectories(uploadsBase);
        } catch (IOException e) {
            // non blocchiamo l'avvio: la directory potrebbe essere creata dal container/host
            log("Impossibile creare uploadsBase: " + uploadsBase + " -> " + e.getMessage());
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

            if (plan == null && (pathParam == null || pathParam.isBlank())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Scheda non trovata.");
                return;
            }

            // Authorization
            boolean allowed = false;
            if (user != null) {
                String ruolo = user.getRuolo();
                if ("PROPRIETARIO".equals(ruolo)) {
                    allowed = true;
                } else if ("PERSONALE".equals(ruolo)) {
                    if (plan != null && plan.getCreatedBy() != null && plan.getCreatedBy().equals(user.getId())) allowed = true;
                } else if ("CLIENTE".equals(ruolo)) {
                    List<TrainingPlanAssignment> assigns = planDao.listAssignmentsForUser(user.getId());
                    for (TrainingPlanAssignment a : assigns) {
                        if (plan != null && a.getPlanId() == plan.getId()) { allowed = true; break; }
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

            // --- resolve percorso file su disco in modo robusto ---
            Path fileOnDisk;
            String filename = null;
            String contentType = null;

            if (plan != null && plan.getAttachmentPath() != null && !plan.getAttachmentPath().isBlank()) {
                String stored = plan.getAttachmentPath().trim();
                // normalizza rimuovendo leading slash
                String normalized = stored.replaceFirst("^/+", "");

                // Se il campo in DB contiene "uploads/..." o "training_plans/..."
                if (normalized.startsWith("uploads/")) {
                    String tail = normalized.substring("uploads/".length());
                    fileOnDisk = uploadDirPath.resolve(tail).normalize();
                } else if (normalized.startsWith("training_plans/")) {
                    fileOnDisk = uploadDirPath.resolve(normalized).normalize();
                } else {
                    // se il valore contiene "training_plans/" in posizione arbitraria (es. "/var/www/uploads/...")
                    int idx = normalized.indexOf("training_plans/");
                    if (idx >= 0) {
                        String tail = normalized.substring(idx);
                        fileOnDisk = uploadDirPath.resolve(tail).normalize();
                    } else {
                        // fallback: consideriamo filename relativo dentro uploadsBase
                        fileOnDisk = uploadsBase.resolve(normalized).normalize();
                    }
                }

                final Path resolvedPath = fileOnDisk;
                filename = Optional.ofNullable(plan.getAttachmentFilename())
                                   .filter(s -> !s.isBlank())
                                   .orElseGet(() -> {
                                       try { return resolvedPath.getFileName().toString(); } catch (Exception ex) { return "attachment"; }
                                   });
                contentType = plan.getAttachmentContentType();
            } else if (pathParam != null && !pathParam.isBlank()) {
                String p = pathParam.trim().replaceFirst("^/+", "");
                if (p.startsWith("uploads/")) {
                    String tail = p.substring("uploads/".length());
                    fileOnDisk = uploadDirPath.resolve(tail).normalize();
                } else if (p.startsWith("training_plans/")) {
                    fileOnDisk = uploadDirPath.resolve(p).normalize();
                } else {
                    fileOnDisk = uploadsBase.resolve(Paths.get(p).getFileName().toString()).normalize();
                }
                filename = fileOnDisk.getFileName().toString();
            } else {
                fileOnDisk = null;
            }

            // safety checks
            if (fileOnDisk == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato.");
                return;
            }
            if (!Files.exists(fileOnDisk) || !Files.isRegularFile(fileOnDisk) || !Files.isReadable(fileOnDisk)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato o non leggibile.");
                return;
            }

            Path fileReal;
            try {
                fileReal = fileOnDisk.toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato.");
                return;
            }

            // garantiamo che il file sia dentro la cartella upload montata (uploadDirPath)
            if (!fileReal.startsWith(uploadDirPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso al file non permesso.");
                return;
            }

            if (contentType == null || contentType.isBlank()) {
                try { contentType = Files.probeContentType(fileReal); } catch (IOException ignored) {}
            }
            if (contentType == null) contentType = "application/octet-stream";

            long fileSize = Files.size(fileReal);

            resp.setContentType(contentType);
            resp.setHeader("Content-Length", String.valueOf(fileSize));
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
            String disposition = "attachment; filename=\"" + filename.replace("\"", "_") + "\"; filename*=UTF-8''" + encoded;
            resp.setHeader("Content-Disposition", disposition);

            try (InputStream in = Files.newInputStream(fileReal, StandardOpenOption.READ);
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
