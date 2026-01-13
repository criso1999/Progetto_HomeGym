package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/posts/comment")
public class AddCommentServlet extends HttpServlet {
    private PostDAO postDao;

    @Override
    public void init() throws ServletException {
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Utente u = (Utente) req.getSession().getAttribute("user");
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        String postId = req.getParameter("postId");
        String text = req.getParameter("text");
        if (postId == null || postId.isBlank() || text == null || text.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }
        postDao.addComment(new ObjectId(postId), u.getId(), u.getNome() + " " + u.getCognome(), text);
        resp.sendRedirect(req.getContextPath() + "/posts/view?id=" + postId);
    }
}
