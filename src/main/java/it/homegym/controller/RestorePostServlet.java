package it.homegym.controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bson.types.ObjectId;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;

@WebServlet("/admin/posts/restore")
public class RestorePostServlet extends HttpServlet {

    private PostDAO postDao;

    @Override
    public void init() {
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();

        // controllo sessione / login
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(ctx + "/login");
            return;
        }

        Utente u = (Utente) session.getAttribute("user");
        if (u == null || !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String postId = req.getParameter("postId");
        String redirectTo = ctx + "/staff/community"; // ritorno alla stessa pagina

        if (postId == null || postId.isBlank()) {
            session.setAttribute("flashError", "ID post mancante.");
            resp.sendRedirect(redirectTo);
            return;
        }

        ObjectId oid;
        try {
            oid = new ObjectId(postId);
        } catch (IllegalArgumentException iae) {
            session.setAttribute("flashError", "ID post non valido.");
            resp.sendRedirect(redirectTo);
            return;
        }

        try {
            boolean ok = postDao.restorePost(oid);
            if (ok) {
                session.setAttribute("flashSuccess", "Post ripristinato e reso visibile.");
            } else {
                session.setAttribute("flashError", "Impossibile ripristinare il post (forse non esiste o già pubblico).");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Errore interno durante il ripristino del post.");
        }

        resp.sendRedirect(redirectTo);
    }
}
