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
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/staff/plans/action")
@MultipartConfig(
    fileSizeThreshold = 1024 * 100, // 100KB
    maxFileSize = 1024 * 1024 * 10, // 10MB per file
    maxRequestSize = 1024 * 1024 * 50 // 50MB total
)
public class TrainingPlanActionServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(TrainingPlanActionServlet.class.getName());

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

        boolean anySaved = false;
        StringBuilder errors = new StringBuilder();

        // Primo tentativo: singolo file input "attachment"
        Part part = null;
        try {
            part = req.getPart("attachment");
        } catch (IllegalStateException | IOException | ServletException ignore) {
            part = null;
        }

        Collection<Part> parts = null;
        if (part == null) {
            // fallback: prendi tutti i part e filtra quelli con filename
            parts = req.getParts();
        }

        // helper lambda-like behaviour (Java 8+: use loops)
        if (part != null) {
            parts = java.util.Collections.singletonList(part);
        }

        for (Part p : parts) {
            if (p == null) continue;
            String submittedName = p.getSubmittedFileName();
            if (submittedName == null || submittedName.isBlank()) continue;

            String filename = Paths.get(submittedName).getFileName().toString();
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
            } catch (IOException ioe) {
                errors.append("Errore salvataggio file: ").append(filename).append(". ");
                continue;
            }

            long size = p.getSize();
            String contentType = p.getContentType();
            String storedPath = "/uploads/training_plans/" + planId + "/" + target.getFileName().toString();

            boolean savedToDb = false;
            try {
                savedToDb = dao.addAttachment(planId, filename, storedPath, size, contentType);
            } catch (SQLException sqle) {
                // log per debug
                log("Errore addAttachment: " + sqle.getMessage(), sqle);
                savedToDb = false;
            }

            // Aggiorna comunque i campi della tabella training_plan per garantire visibilità lato UI
            try {
                TrainingPlan toUpdate = dao.findById(planId);
                if (toUpdate != null) {
                    toUpdate.setAttachmentFilename(filename);
                    toUpdate.setAttachmentPath(storedPath);
                    toUpdate.setAttachmentContentType(contentType);
                    toUpdate.setAttachmentSize(size);
                    dao.update(toUpdate); // aggiorna i campi attachment_*
                }
            } catch (SQLException e) {
                log("Errore aggiornamento training_plan attachment fields: " + e.getMessage(), e);
                // non blocchiamo l'upload; segnaliamo comunque l'errore
                errors.append("Errore registrazione meta su training_plan per file: ").append(filename).append(". ");
            }

            if (savedToDb) anySaved = true;
            else {
                // se addAttachment non ha inserito in training_plan_attachment, ma abbiamo aggiornato training_plan, consideriamo comunque salvato
                if (anySaved == false) {
                    // anySaved rimane false finché non incontriamo un savedToDb === true,
                    // ma la presenza di aggiornamento a training_plan è stata tentata sopra indipendentemente.
                }
            }
        }

        if (anySaved) {
            session.setAttribute("flashSuccess", "Allegati caricati.");
            if (errors.length() > 0) session.setAttribute("flashError", errors.toString());
        } else {
            // se non abbiamo scritto su attachment table ma l'update training_plan è andato a buon fine,
            // potremmo comunque desiderare segnalarlo come successo; decide tu come preferisci.
            if (errors.length() > 0) {
                session.setAttribute("flashError", errors.toString());
            } else {
                session.setAttribute("flashError", "Nessun allegato caricato.");
            }
        }
    }

}
