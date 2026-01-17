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
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Utente u = (Utente) session.getAttribute("user");
        String postId = req.getParameter("postId");

        if (postId == null || postId.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }

        ObjectId oid;
        try {
            oid = new ObjectId(postId);
        } catch (IllegalArgumentException e) {
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }

        Document post = postDao.findById(oid);
        if (post == null) {
            resp.sendRedirect(req.getContextPath() + "/posts");
            return;
        }

        int authorId = post.getInteger("userId");

        boolean isAdmin = "PROPRIETARIO".equals(u.getRuolo());
        boolean isAuthor = authorId == u.getId();

        if (!isAdmin && !isAuthor) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Non sei autorizzato a eliminare questo post");
            return;
        }

        postDao.deletePost(oid);
        session.setAttribute("flashSuccess", "Post eliminato con successo");
        if(isAdmin || isAuthor){
            resp.sendRedirect(req.getContextPath() + "/staff/community");
        }
        else{
            resp.sendRedirect(req.getContextPath() + "/posts");
        }
        
    }
}
