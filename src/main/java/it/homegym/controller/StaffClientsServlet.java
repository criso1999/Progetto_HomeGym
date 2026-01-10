package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/clients")
public class StaffClientsServlet extends HttpServlet {

    private UtenteDAO dao;

    @Override
    public void init() throws ServletException {
        try {
            dao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // controllo ruolo (in più al filtro)
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            List<Utente> clients = dao.listByRole("CLIENTE");
            req.setAttribute("clients", clients);
            req.getRequestDispatcher("/WEB-INF/views/staff/clients.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
