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

@WebServlet("/posts/evaluate")
public class TrainerEvaluationServlet extends HttpServlet {
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
            resp.sendRedirect(ctx + "/login");
            return;
        }

        Utente u = (Utente) session.getAttribute("user");
        if (!"PERSONALE".equals(u.getRuolo()) && !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String postId = req.getParameter("postId");
        String note = req.getParameter("note");
        String scoreParam = req.getParameter("score");

        if (postId == null || postId.isBlank()) {
            session.setAttribute("flashError", "Post non valido.");
            resp.sendRedirect(ctx + "/posts");
            return;
        }

        int score = 0;
        try { score = Integer.parseInt(scoreParam); } catch (Exception ignored) {}
        if (score < 1 || score > 5) {
            session.setAttribute("flashError", "Score invalido (1-5).");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postId, StandardCharsets.UTF_8) + "#eval");
            return;
        }

        // rate limit per trainer (usa emailLimiter come semplice limiter condiviso)
        String key = "eval:trainer:" + u.getId();
        if (!RateLimitService.emailLimiter.allow(key)) {
            session.setAttribute("flashError", "Troppe richieste. Riprova più tardi.");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postId, StandardCharsets.UTF_8) + "#eval");
            return;
        }

        ObjectId oid;
        try {
            oid = new ObjectId(postId);
        } catch (IllegalArgumentException iae) {
            session.setAttribute("flashError", "ID post non valido.");
            resp.sendRedirect(ctx + "/posts");
            return;
        }

        try {
            String trainerName = ((u.getNome() != null ? u.getNome() : "") + " " + (u.getCognome() != null ? u.getCognome() : "")).trim();
            boolean added = postDao.addTrainerEvaluation(oid, u.getId(), trainerName, note == null ? "" : note.trim(), score);
            if (!added) {
                session.setAttribute("flashError", "Hai già lasciato una valutazione per questo post.");
            } else {
                session.setAttribute("flashSuccess", "Valutazione inserita.");
            }
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postId, StandardCharsets.UTF_8) + "#eval");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("flashError", "Errore durante l'inserimento della valutazione.");
            resp.sendRedirect(ctx + "/posts/view?id=" + URLEncoder.encode(postId, StandardCharsets.UTF_8) + "#eval");
        }
    }
}
