package it.homegym.controller;

import it.homegym.dao.PostDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/staff/community")
public class TrainerCommunityServlet extends HttpServlet {

    private PostDAO postDao;
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            postDao = new PostDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception ex) {
            throw new ServletException("Impossibile inizializzare DAO", ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Utente user = (Utente) s.getAttribute("user");
        if (!"PERSONALE".equals(user.getRuolo()) && !"PROPRIETARIO".equals(user.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
            return;
        }

        try {
            // prendi id clienti assegnati al trainer
            List<Integer> clientIds = utenteDao.listClientIdsByTrainer(user.getId());

            List<Document> posts;
            if (clientIds == null || clientIds.isEmpty()) {
                // fallback: mostra tutti i post (o vuoto) — evita null pointer
                posts = postDao.listFeed(1, 30); // pagina 1, 30 elementi
            } else {
                posts = postDao.listByUserIds(clientIds, 1, 30);
            }

            req.setAttribute("posts", posts);
            req.getRequestDispatcher("/WEB-INF/views/staff/community.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
