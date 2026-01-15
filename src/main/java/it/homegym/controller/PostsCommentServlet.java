package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import it.homegym.security.RateLimitService;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/posts/comment")
public class PostsCommentServlet extends HttpServlet {

    private PostDAO postDao;

    @Override
    public void init() throws ServletException {
        super.init();
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();

        if (session == null || session.getAttribute("user") == null) {
            // non autenticato -> login
            resp.sendRedirect(ctx + "/login");
            return;
        }

        Utente user = (Utente) session.getAttribute("user");
        String postIdParam = req.getParameter("postId");
        String text = req.getParameter("text");

        // validazione minima
        if (postIdParam == null || postIdParam.isBlank() || text == null || text.isBlank()) {
            session.setAttribute("flashError", "Il commento non può essere vuoto.");
            String ret = ctx + "/posts";
            if (postIdParam != null && !postIdParam.isBlank()) {
                ret = ctx + "/posts/view?id=" + URLEncoder.encode(postIdParam, StandardCharsets.UTF_8) + "#comments";
            }
            resp.sendRedirect(ret);
            return;
        }

        // rate limit per utente (usa l'emailLimiter come semplice limiter per chiave utente)
        String userKey = "comment:user:" + user.getId();
        if (!RateLimitService.emailLimiter.allow(userKey)) {
            session.setAttribute("flashError", "Troppe richieste. Riprova più tardi.");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postIdParam, StandardCharsets.UTF_8) + "#comments");
            return;
        }

        // converti id in ObjectId
        ObjectId oid;
        try {
            oid = new ObjectId(postIdParam);
        } catch (IllegalArgumentException iae) {
            session.setAttribute("flashError", "ID post non valido.");
            resp.sendRedirect(ctx + "/posts");
            return;
        }

        // sanitizzazione semplice
        String cleaned = text.trim();
        if (cleaned.length() > 2000) cleaned = cleaned.substring(0, 2000);

        try {
            String userName = ((user.getNome() != null ? user.getNome() : "").trim()
                                + " "
                                + (user.getCognome() != null ? user.getCognome() : "").trim()).trim();
            postDao.addComment(oid, user.getId(), userName, cleaned);

            session.setAttribute("flashSuccess", "Commento aggiunto.");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(oid.toHexString(), StandardCharsets.UTF_8) + "#comments");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Errore durante l'aggiunta del commento.");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postIdParam, StandardCharsets.UTF_8) + "#comments");
        }
    }
}
