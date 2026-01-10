package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/staff")
public class AdminStaffServlet extends HttpServlet {
    private UtenteDAO dao;
    @Override
    public void init() throws ServletException {
        try { dao = new UtenteDAO(); } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Utente> staff = dao.listByRole("PERSONALE");
            req.setAttribute("staffList", staff);
            req.getRequestDispatcher("/WEB-INF/views/admin/staff.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
