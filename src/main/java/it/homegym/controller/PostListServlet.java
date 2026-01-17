package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/posts")
public class PostListServlet extends HttpServlet {
    private PostDAO postDao;
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        try {
            postDao = new PostDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare DAO", e);
        }
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

        List<Document> posts = new ArrayList<>();

        try {
            if (user != null && "PROPRIETARIO".equals(user.getRuolo())) {
                // Admin: vede tutti i post (inclusi hidden)
                posts = postDao.listAllForAdmin(page, pageSize);
            } else if (user != null && "PERSONALE".equals(user.getRuolo())) {
                // Trainer: mostra i post dei suoi clienti (solo visibili = PUBLIC)
                List<Integer> clientIds = utenteDao.listClientIdsByTrainer(user.getId());
                if (clientIds == null || clientIds.isEmpty()) {
                    // fallback: mostra feed pubblico (opzionale) oppure lista vuota
                    posts = postDao.listFeedPublic(page, pageSize);
                    req.setAttribute("info", "Non ci sono clienti assegnati — mostrando il feed pubblico.");
                } else {
                    posts = postDao.listVisibleByUserIds(clientIds, page, pageSize);
                }
            } else {
                // CLIENTE o anonimo: mostra feed pubblico
                posts = postDao.listFeedPublic(page, pageSize);
            }
        } catch (Exception e) {
            throw new ServletException("Errore caricamento feed posts", e);
        }

        // Normalizza gli _id (ObjectId -> hex string) per la JSP e crea _idStr
        for (Document p : posts) {
            Object id = p.get("_id");
            if (id instanceof ObjectId) {
                String hex = ((ObjectId) id).toHexString();
                p.put("_id", hex);
                p.put("_idStr", hex);
            } else if (id != null) {
                String s = id.toString();
                p.put("_idStr", s);
            } else {
                p.put("_idStr", "");
            }
        }

        req.setAttribute("posts", posts);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", pageSize);

        req.getRequestDispatcher("/WEB-INF/views/posts/feed.jsp")
           .forward(req, resp);
    }
}
