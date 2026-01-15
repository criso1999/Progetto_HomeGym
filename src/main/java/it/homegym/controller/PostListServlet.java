package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/posts")
public class PostListServlet extends HttpServlet {

    private PostDAO postDao;

    @Override
    public void init() throws ServletException {
        postDao = new PostDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int page = 1;
        int pageSize = 10;

        try {
            String p = req.getParameter("page");
            if (p != null) page = Math.max(1, Integer.parseInt(p));
        } catch (Exception ignored) {}

        HttpSession session = req.getSession(false);
        Utente user = session != null ? (Utente) session.getAttribute("user") : null;

        List<Document> posts;

        if (user != null && "PROPRIETARIO".equals(user.getRuolo())) {
            posts = postDao.listAllForAdmin(page, pageSize);
        } else {
            posts = postDao.listFeedPublic(page, pageSize);
        }

        // 🔑 NORMALIZZAZIONE ID
        for (Document p : posts) {
            Object id = p.get("_id");
            if (id instanceof ObjectId) {
                p.put("_idStr", ((ObjectId) id).toHexString());
            }
        }

        req.setAttribute("posts", posts);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", pageSize);

        req.getRequestDispatcher("/WEB-INF/views/posts/feed.jsp")
           .forward(req, resp);
    }
}
