package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Collection;

@WebServlet("/staff/plans/action")
@MultipartConfig(
    fileSizeThreshold = 1024 * 100, // 100KB
    maxFileSize = 1024 * 1024 * 10, // 10MB per file
    maxRequestSize = 1024 * 1024 * 50 // 50MB total
)
public class TrainingPlanActionServlet extends HttpServlet {

    private TrainingPlanDAO dao;
    // path base dove salvare gli allegati (configurabile via env)
    private Path attachmentsBase;

    @Override
    public void init() throws ServletException {
        try {
            dao = new TrainingPlanDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare TrainingPlanDAO", e);
        }
        String uploadDir = System.getenv().getOrDefault("UPLOAD_DIR", "/tmp/homegym_uploads");
        attachmentsBase = Paths.get(uploadDir, "training_plans");
        try {
            Files.createDirectories(attachmentsBase);
        } catch (IOException e) {
            throw new ServletException("Impossibile creare cartella upload: " + attachmentsBase, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        Utente user = s != null ? (Utente) s.getAttribute("user") : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"PERSONALE".equals(user.getRuolo()) && !"PROPRIETARIO".equals(user.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "create":
                    handleCreate(req, s, user);
                    break;
                case "update":
                    handleUpdate(req, s, user);
                    break;
                case "delete":
                    handleDelete(req, s, user);
                    break;
                case "upload":
                    handleUpload(req, s, user);
                    break;
                default:
                    s.setAttribute("flashError", "Azione non riconosciuta.");
            }
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void handleCreate(HttpServletRequest req, HttpSession session, Utente user) throws SQLException {
        String title = req.getParameter("title");
        String description = req.getParameter("description");
        String content = req.getParameter("content");
        if (title == null || title.isBlank()) {
            session.setAttribute("flashError", "Titolo obbligatorio.");
            return;
        }
        TrainingPlan tp = new TrainingPlan();
        tp.setTitle(title);
        tp.setDescription(description);
        tp.setContent(content);
        tp.setCreatedBy(user.getId());
        int newId = dao.create(tp); // implementa create che ritorna id
        if (newId > 0) {
            session.setAttribute("flashSuccess", "Scheda creata (id: " + newId + ").");
        } else {
            session.setAttribute("flashError", "Errore creazione scheda.");
        }
    }

    private void handleUpdate(HttpServletRequest req, HttpSession session, Utente user) throws SQLException {
        String idS = req.getParameter("id");
        if (idS == null || idS.isBlank()) { session.setAttribute("flashError", "ID mancante."); return; }
        int id = Integer.parseInt(idS);
        TrainingPlan existing = dao.findById(id);
        if (existing == null) { session.setAttribute("flashError", "Scheda non trovata."); return; }

        // permission: solo creator o owner (PROPRIETARIO) può aggiornare
        if (!(user.getId() == existing.getCreatedBy() || "PROPRIETARIO".equals(user.getRuolo()))) {
            session.setAttribute("flashError", "Non autorizzato ad aggiornare questa scheda.");
            return;
        }

        existing.setTitle(req.getParameter("title"));
        existing.setDescription(req.getParameter("description"));
        existing.setContent(req.getParameter("content"));
        boolean ok = dao.update(existing);
        session.setAttribute("flashSuccess", ok ? "Scheda aggiornata." : "Errore durante aggiornamento.");
    }

    private void handleDelete(HttpServletRequest req, HttpSession session, Utente user) throws SQLException {
        String idS = req.getParameter("id");
        if (idS == null || idS.isBlank()) { session.setAttribute("flashError", "ID mancante."); return; }
        int id = Integer.parseInt(idS);
        TrainingPlan existing = dao.findById(id);
        if (existing == null) { session.setAttribute("flashError", "Scheda non trovata."); return; }

        if (!(user.getId() == existing.getCreatedBy() || "PROPRIETARIO".equals(user.getRuolo()))) {
            session.setAttribute("flashError", "Non autorizzato ad eliminare questa scheda.");
            return;
        }

        boolean ok = dao.softDelete(id);
        session.setAttribute("flashSuccess", ok ? "Scheda rimossa (soft-delete)." : "Errore rimozione scheda.");
    }

    private void handleUpload(HttpServletRequest req, HttpSession session, Utente user) throws Exception {
        // expected param: planId
        String idS = req.getParameter("planId");
        if (idS == null || idS.isBlank()) {
            session.setAttribute("flashError", "ID piano mancante per upload.");
            return;
        }
        int planId = Integer.parseInt(idS);
        TrainingPlan plan = dao.findById(planId);
        if (plan == null) { session.setAttribute("flashError", "Piano non trovato."); return; }

        if (!(user.getId() == plan.getCreatedBy() || "PROPRIETARIO".equals(user.getRuolo()))) {
            session.setAttribute("flashError", "Non autorizzato a caricare allegati per questo piano.");
            return;
        }

        Collection<Part> parts = req.getParts();
        boolean anySaved = false;
        StringBuilder errors = new StringBuilder();
        for (Part p : parts) {
            if (p.getName() == null) continue;
            if (p.getSubmittedFileName() == null) continue;
            String filename = Paths.get(p.getSubmittedFileName()).getFileName().toString();
            // whitelist extensions
            String lower = filename.toLowerCase();
            if (!(lower.endsWith(".pdf") || lower.endsWith(".xls") || lower.endsWith(".xlsx"))) {
                errors.append("Solo PDF/Excel permessi. File ignorato: ").append(filename).append(". ");
                continue;
            }
            Path planDir = attachmentsBase.resolve(String.valueOf(planId));
            Files.createDirectories(planDir);
            Path target = planDir.resolve(System.currentTimeMillis() + "_" + filename);
            try (InputStream in = p.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // salva meta e path nel DB (includendo size e contentType)
            long size = p.getSize();
            String contentType = p.getContentType();
            String storedPath = "/uploads/training_plans/" + planId + "/" + target.getFileName().toString();

            boolean savedToDb;
            try {
                savedToDb = dao.addAttachment(planId, filename, storedPath, size, contentType);
            } catch (SQLException sqle) {
                savedToDb = false;
            }

            if (savedToDb) {
                anySaved = true;
            } else {
                errors.append("Errore registrazione DB per file: ").append(filename).append(". ");
            }
        }

        if (anySaved) {
            session.setAttribute("flashSuccess", "Allegati caricati.");
            if (errors.length() > 0) session.setAttribute("flashError", errors.toString());
        } else {
            session.setAttribute("flashError", errors.length() > 0 ? errors.toString() : "Nessun allegato caricato.");
        }
    }
}
