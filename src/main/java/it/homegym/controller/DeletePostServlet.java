package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/posts/delete")
public class DeletePostServlet extends HttpServlet {

    private PostDAO postDao;

    @Override
    public void init() {
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();

        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(ctx + "/login");
            return;
        }

        Utente u = (Utente) session.getAttribute("user");
        String postId = req.getParameter("postId");

        if (postId == null || postId.isBlank()) {
            session.setAttribute("flashError", "ID post mancante.");
            resp.sendRedirect(ctx + "/posts");
            return;
        }

        ObjectId oid;
        try {
            oid = new ObjectId(postId);
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashError", "ID post non valido.");
            resp.sendRedirect(ctx + "/posts");
            return;
        }

        try {
            Document post = postDao.findById(oid);
            if (post == null) {
                session.setAttribute("flashError", "Post non trovato.");
                // redirect intelligente in base al ruolo
                if ("PERSONALE".equals(u.getRuolo()) || "PROPRIETARIO".equals(u.getRuolo())) {
                    resp.sendRedirect(ctx + "/staff/community");
                } else {
                    resp.sendRedirect(ctx + "/client/profile");
                }
                return;
            }

            // estrai authorId in modo difensivo (evitiamo ClassCastException)
            Integer authorId = null;
            Object authorObj = post.get("userId");
            if (authorObj instanceof Number) {
                authorId = ((Number) authorObj).intValue();
            } else if (authorObj != null) {
                try {
                    authorId = Integer.parseInt(authorObj.toString());
                } catch (NumberFormatException ignored) { authorId = null; }
            }

            boolean isAdmin = "PROPRIETARIO".equals(u.getRuolo());
            boolean isAuthor = (authorId != null && authorId.equals(u.getId()));

            if (!isAdmin && !isAuthor) {
                // non autorizzato -> non generiamo 5xx. Forniamo un messaggio e redirect sensato.
                session.setAttribute("flashError", "Non sei autorizzato a eliminare questo post.");
                if ("PERSONALE".equals(u.getRuolo()) || "PROPRIETARIO".equals(u.getRuolo())) {
                    resp.sendRedirect(ctx + "/staff/community");
                } else {
                    resp.sendRedirect(ctx + "/client/profile");
                }
                return;
            }

            boolean deleted = postDao.deletePost(oid);
            if (deleted) {
                session.setAttribute("flashSuccess", "Post eliminato con successo.");
            } else {
                session.setAttribute("flashError", "Impossibile eliminare il post (forse è già stato rimosso).");
            }

            // redirect sensato: admin/staff -> staff/community, clienti autori -> client/profile, altrimenti /posts
            if (isAdmin || "PERSONALE".equals(u.getRuolo())) {
                resp.sendRedirect(ctx + "/staff/community");
            } else if (isAuthor && "CLIENTE".equals(u.getRuolo())) {
                resp.sendRedirect(ctx + "/client/profile");
            } else {
                resp.sendRedirect(ctx + "/posts");
            }

        } catch (Exception e) {
            // log (stacktrace) e messaggio amichevole
            e.printStackTrace();
            session.setAttribute("flashError", "Errore interno durante l'eliminazione del post.");
            // redirect fallback
            if ("PERSONALE".equals(u.getRuolo()) || "PROPRIETARIO".equals(u.getRuolo())) {
                resp.sendRedirect(ctx + "/staff/community");
            } else {
                resp.sendRedirect(ctx + "/client/profile");
            }
        }
    }
}
