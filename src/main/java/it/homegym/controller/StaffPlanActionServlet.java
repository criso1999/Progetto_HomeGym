package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.UUID;

@WebServlet("/staff/plans/action")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1MB
    maxFileSize = 10 * 1024 * 1024,  // 10MB per file
    maxRequestSize = 20 * 1024 * 1024
)
public class StaffPlanActionServlet extends HttpServlet {

    private TrainingPlanDAO dao;
    // cartella di upload (preferibile metterla in config/env)
    private Path uploadBase;

    @Override
    public void init() throws ServletException {
        try {
            dao = new TrainingPlanDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
        // default upload folder: ${catalina.base}/uploads/plans
        String base = System.getenv().getOrDefault("UPLOAD_BASE", null);
        if (base == null) {
            String catalina = System.getProperty("catalina.base", System.getProperty("java.io.tmpdir"));
            base = catalina + File.separator + "uploads" + File.separator + "plans";
        }
        uploadBase = Paths.get(base);
        try { Files.createDirectories(uploadBase); } catch (IOException ignored) {}
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        Utente u = s != null ? (Utente) s.getAttribute("user") : null;
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        if (!"PERSONALE".equals(u.getRuolo()) && !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }

        String action = req.getParameter("action");
        try {
            // read simple params
            String title = req.getParameter("title");
            String description = req.getParameter("description");
            String content = req.getParameter("content");

            // file part (may be null)
            Part filePart = null;
            try {
                filePart = req.getPart("attachment");
            } catch (IllegalStateException ise) {
                // file too large
                req.getSession().setAttribute("flashError", "File troppo grande.");
                resp.sendRedirect(req.getContextPath() + "/staff/plans");
                return;
            }

            // allowed content types
            String[] allowed = new String[] {
                    "application/pdf",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };

            if ("create".equals(action)) {
                TrainingPlan p = new TrainingPlan();
                p.setTitle(title);
                p.setDescription(description);
                p.setContent(content);
                p.setCreatedBy(u.getId());

                // handle file
                if (filePart != null && filePart.getSize() > 0) {
                    String contentType = filePart.getContentType();
                    boolean okType = false;
                    for (String a : allowed) if (a.equalsIgnoreCase(contentType)) { okType = true; break; }
                    if (!okType) {
                        req.getSession().setAttribute("flashError", "Tipo file non supportato.");
                        resp.sendRedirect(req.getContextPath() + "/staff/plans");
                        return;
                    }
                    String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                    String ext = "";
                    int i = submitted.lastIndexOf('.');
                    if (i > 0) ext = submitted.substring(i);
                    String fname = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + ext;
                    Path target = uploadBase.resolve(fname);
                    try (InputStream in = filePart.getInputStream()) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    p.setAttachmentFilename(submitted);
                    p.setAttachmentContentType(contentType);
                    p.setAttachmentPath(target.toString());
                    p.setAttachmentSize(filePart.getSize());
                }

                int newId = dao.createPlan(p); // implementa create che salva anche attachment metadata
                req.getSession().setAttribute("flashSuccess", "Scheda creata.");
                resp.sendRedirect(req.getContextPath() + "/staff/plans");

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                TrainingPlan p = dao.findById(id);
                if (p == null) { req.getSession().setAttribute("flashError", "Scheda non trovata."); resp.sendRedirect(req.getContextPath()+"/staff/plans"); return; }

                // permission: solo creatore o owner può aggiornare
                if (!"PROPRIETARIO".equals(u.getRuolo()) && (p.getCreatedBy() == null || !p.getCreatedBy().equals(u.getId()))) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN); return;
                }

                p.setTitle(title);
                p.setDescription(description);
                p.setContent(content);

                // remove existing attachment?
                String remove = req.getParameter("removeAttachment");
                if ("1".equals(remove) && p.getAttachmentPath() != null) {
                    try { Files.deleteIfExists(Paths.get(p.getAttachmentPath())); } catch (Exception ignored) {}
                    p.setAttachmentFilename(null);
                    p.setAttachmentContentType(null);
                    p.setAttachmentPath(null);
                    p.setAttachmentSize(null);
                }

                // new upload replaces previous
                if (filePart != null && filePart.getSize() > 0) {
                    String contentType = filePart.getContentType();
                    boolean okType = false;
                    for (String a : allowed) if (a.equalsIgnoreCase(contentType)) { okType = true; break; }
                    if (!okType) {
                        req.getSession().setAttribute("flashError", "Tipo file non supportato.");
                        resp.sendRedirect(req.getContextPath() + "/staff/plans");
                        return;
                    }
                    // delete old
                    if (p.getAttachmentPath() != null) {
                        try { Files.deleteIfExists(Paths.get(p.getAttachmentPath())); } catch (Exception ignored) {}
                    }
                    String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                    String ext = "";
                    int i = submitted.lastIndexOf('.');
                    if (i > 0) ext = submitted.substring(i);
                    String fname = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + ext;
                    Path target = uploadBase.resolve(fname);
                    try (InputStream in = filePart.getInputStream()) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    p.setAttachmentFilename(submitted);
                    p.setAttachmentContentType(contentType);
                    p.setAttachmentPath(target.toString());
                    p.setAttachmentSize(filePart.getSize());
                }

                dao.update(p);
                req.getSession().setAttribute("flashSuccess", "Scheda aggiornata.");
                resp.sendRedirect(req.getContextPath() + "/staff/plans");
            } else {
                resp.sendRedirect(req.getContextPath() + "/staff/plans");
            }
        } catch (SQLException sqle) {
            throw new ServletException(sqle);
        }
    }
}

