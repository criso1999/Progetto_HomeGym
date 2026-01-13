package it.homegym.controller;

import it.homegym.dao.PostDAO;
import org.bson.Document;

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int page = 1;
        int pageSize = 10;
        String p = req.getParameter("page");
        try { if (p != null) page = Math.max(1, Integer.parseInt(p)); } catch (Exception ignored) {}

        List<Document> posts = postDao.listFeed(page, pageSize);
        req.setAttribute("posts", posts);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", pageSize);
        req.getRequestDispatcher("/WEB-INF/views/posts/feed.jsp").forward(req, resp);
    }
}
