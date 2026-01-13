package it.homegym.controller;

import it.homegym.dao.MediaDAO;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.OutputStream;

@WebServlet("/media/*")
public class MediaServlet extends HttpServlet {
    private MediaDAO dao;

    @Override
    public void init() throws ServletException {
        dao = new MediaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String path = req.getPathInfo(); // /{objectId}
            if (path == null || path.length() <= 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String id = path.substring(1);
            ObjectId oid = new ObjectId(id);
            resp.setHeader("Cache-Control", "public, max-age=86400");
            try (OutputStream os = resp.getOutputStream()) {
                dao.streamTo(oid, os);
            }
        } catch (IllegalArgumentException iae) {
            try { resp.sendError(HttpServletResponse.SC_NOT_FOUND); } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
            try { resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); } catch (Exception ignored) {}
        }
    }
}
