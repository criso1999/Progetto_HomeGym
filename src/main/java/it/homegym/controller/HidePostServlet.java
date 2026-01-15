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

@WebServlet("/admin/posts/hide")
public class HidePostServlet extends HttpServlet {

    private PostDAO postDao;

    @Override
    public void init() {
        postDao = new PostDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);
        Utente u = (Utente) session.getAttribute("user");

        if (u == null || !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String postId = req.getParameter("postId");
        String reason = req.getParameter("reason");

        ObjectId oid = new ObjectId(postId);
        postDao.hidePost(oid, u.getId(), reason);

        session.setAttribute("flashSuccess", "Post nascosto dalla community");
        resp.sendRedirect(req.getContextPath() + "/admin/community");
    }
}

