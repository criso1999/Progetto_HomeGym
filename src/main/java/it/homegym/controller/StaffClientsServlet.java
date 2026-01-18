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
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Utente current = (Utente) s.getAttribute("user");

        try {
            List<Utente> clients;
            // se il loggato è PERSONALE mostra solo i suoi clienti
            if ("PERSONALE".equals(current.getRuolo())) {
                clients = dao.listClientsByTrainer(current.getId());
                // disponibili per l'assign: clienti senza trainer
                List<Utente> available = dao.listAvailableClientsForAssign();
                req.setAttribute("availableClients", available);
            } else {
                // PROPRIETARIO / admin: mostra tutti i clienti (non deleted)
                clients = dao.listByRole("CLIENTE");
                // admin può anche vedere la lista disponibile per assign
                List<Utente> available = dao.listAvailableClientsForAssign();
                req.setAttribute("availableClients", available);
            }

            req.setAttribute("clients", clients);
            req.setAttribute("currentUser", current);
            req.getRequestDispatcher("/WEB-INF/views/staff/clients.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
