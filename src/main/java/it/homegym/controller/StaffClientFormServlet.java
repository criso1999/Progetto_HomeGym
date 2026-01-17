package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/clients/form")
public class StaffClientFormServlet extends HttpServlet {
    private UtenteDAO dao;
    @Override
    public void init() throws ServletException {
        try { dao = new UtenteDAO(); } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        try {
            // lista trainer per la select
            List<Utente> trainers = dao.listByRole("PERSONALE");
            req.setAttribute("trainers", trainers);

            if (id != null && !id.isBlank()) {
                Utente u = dao.findById(Integer.parseInt(id));
                req.setAttribute("client", u);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/views/staff/client-form.jsp").forward(req, resp);
    }
}
