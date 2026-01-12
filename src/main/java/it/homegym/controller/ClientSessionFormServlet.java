package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/client/sessions/new")
public class ClientSessionFormServlet extends HttpServlet {

    private UtenteDAO utenteDAO;

    @Override
    public void init() throws ServletException {
        try {
            utenteDAO = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private HttpSession requireSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return s;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        try {
            List<Utente> trainers = utenteDAO.listByRole("PERSONALE");
            req.setAttribute("trainers", trainers);
            req.setAttribute("scheduledAtInput", ""); // vuoto per nuovo
            req.getRequestDispatcher("/WEB-INF/views/client/session-form.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
