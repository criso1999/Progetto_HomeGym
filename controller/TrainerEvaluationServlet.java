package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/posts/evaluate")
public class TrainerEvaluationServlet extends HttpServlet {
    private PostDAO postDao;

    @Override
    public void init() throws ServletException {
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Utente u = (Utente) req.getSession().getAttribute("user");
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        // role check: only PERSONALE or PROPRIETARIO
        if (!"PERSONALE".equals(u.getRuolo()) && !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String postId = req.getParameter("postId");
        String note = req.getParameter("note");
        int score = 0;
        try { score = Integer.parseInt(req.getParameter("score")); } catch (Exception ignored) {}

        postDao.addTrainerEvaluation(new ObjectId(postId), u.getId(), u.getNome() + " " + u.getCognome(), note, score);
        resp.sendRedirect(req.getContextPath() + "/posts/view?id=" + postId);
    }
}
