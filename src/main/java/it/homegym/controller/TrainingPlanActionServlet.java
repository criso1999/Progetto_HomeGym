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
import java.util.Collections;
import java.util.logging.Logger;

@WebServlet("/staff/plans/action")
@MultipartConfig(
        fileSizeThreshold = 1024 * 100,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 50
)
public class TrainingPlanActionServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(TrainingPlanActionServlet.class.getName());

    private TrainingPlanDAO dao;
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

    // -----------------------------------------------------

    private void handleCreate(HttpServletRequest req, HttpSession session, Utente user) throws Exception {

        String title = req.getParameter("title");

        if (title == null || title.isBlank()) {
            session.setAttribute("flashError", "Titolo obbligatorio.");
            return;
        }

        TrainingPlan tp = new TrainingPlan();
        tp.setTitle(title);
        tp.setDescription(req.getParameter("description"));
        tp.setContent(req.getParameter("content"));
        tp.setCreatedBy(user.getId());

        int newId = dao.create(tp);

        if (newId <= 0) {
            session.setAttribute("flashError", "Errore creazione scheda.");
            return;
        }

        try {
            Part part = req.getPart("attachment");
            if (part != null && part.getSize() > 0) {
                saveAttachmentForPlan(part, newId, session);
            }
        } catch (Exception ignore) {}

        session.setAttribute("flashSuccess", "Scheda creata.");
    }

    private void handleUpdate(HttpServletRequest req, HttpSession session, Utente user) throws SQLException {

        String idS = req.getParameter("id");
        if (idS == null || idS.isBlank()) return;

        int id = Integer.parseInt(idS);
        TrainingPlan existing = dao.findById(id);
        if (existing == null) return;

        existing.setTitle(req.getParameter("title"));
        existing.setDescription(req.getParameter("description"));
        existing.setContent(req.getParameter("content"));

        dao.update(existing);
        session.setAttribute("flashSuccess", "Scheda aggiornata.");
    }

    private void handleDelete(HttpServletRequest req, HttpSession session, Utente user) throws SQLException {

        String idS = req.getParameter("id");
        if (idS == null || idS.isBlank()) return;

        int id = Integer.parseInt(idS);
        dao.softDelete(id);
        session.setAttribute("flashSuccess", "Scheda rimossa.");
    }

    private void handleUpload(HttpServletRequest req, HttpSession session, Utente user) throws Exception {

        String idS = req.getParameter("planId");

        if (idS == null || idS.isBlank()) {
            logger.warning("planId missing in upload");
            session.setAttribute("flashError", "ID piano mancante.");
            return;
        }

        int planId = Integer.parseInt(idS);

        Part part = req.getPart("attachment");

        if (part == null || part.getSize() == 0) {
            session.setAttribute("flashError", "Nessun file selezionato.");
            return;
        }

        saveAttachmentForPlan(part, planId, session);
        session.setAttribute("flashSuccess", "Allegato caricato.");
    }

    // -----------------------------------------------------

    private void saveAttachmentForPlan(Part p, int planId, HttpSession session) throws Exception {

        String submittedName = p.getSubmittedFileName();
        if (submittedName == null) return;

        String filename = Paths.get(submittedName).getFileName().toString();
        String lower = filename.toLowerCase();

        if (!(lower.endsWith(".pdf") || lower.endsWith(".xls") || lower.endsWith(".xlsx"))) {
            session.setAttribute("flashError", "Formato non valido.");
            return;
        }

        Path planDir = attachmentsBase.resolve(String.valueOf(planId));
        Files.createDirectories(planDir);

        Path target = planDir.resolve(System.currentTimeMillis() + "_" + filename);

        try (InputStream in = p.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        long size = p.getSize();
        String contentType = p.getContentType();
        String storedPath = "/uploads/training_plans/" + planId + "/" + target.getFileName();

        dao.addAttachment(planId, filename, storedPath, size, contentType);

        TrainingPlan toUpdate = dao.findById(planId);
        if (toUpdate != null) {
            toUpdate.setAttachmentFilename(filename);
            toUpdate.setAttachmentPath(storedPath);
            toUpdate.setAttachmentContentType(contentType);
            toUpdate.setAttachmentSize(size);
            dao.update(toUpdate);
        }

        logger.info("Upload OK: " + target);
    }
}
