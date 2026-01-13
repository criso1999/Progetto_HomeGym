package it.homegym.controller;

import it.homegym.dao.MediaDAO;
import it.homegym.dao.PostDAO;
import it.homegym.model.Utente;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@WebServlet("/posts/create")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 50 * 1024 * 1024L, // 50MB
        maxRequestSize = 200 * 1024 * 1024L) // 200MB
public class CreatePostServlet extends HttpServlet {
    private PostDAO postDao;
    private MediaDAO mediaDao;

    @Override
    public void init() throws ServletException {
        postDao = new PostDAO();
        mediaDao = new MediaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        req.getRequestDispatcher("/WEB-INF/views/posts/create.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
        Utente user = (Utente) req.getSession().getAttribute("user");
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        String content = req.getParameter("content");
        List<Document> medias = new ArrayList<>();

        Collection<Part> parts = req.getParts();
        for (Part p : parts) {
            if (p.getName().equals("media") && p.getSize() > 0) {
                String filename = p.getSubmittedFileName();
                String contentType = p.getContentType();
                try (InputStream in = p.getInputStream()) {
                    ObjectId fileId = mediaDao.upload(in, filename, contentType);
                    medias.add(new Document("fileId", fileId).append("filename", filename).append("contentType", contentType));
                }
            }
        }

        postDao.createPost(user.getId(), user.getNome() + " " + user.getCognome(), content, medias);
        resp.sendRedirect(req.getContextPath() + "/posts");
    }
}
