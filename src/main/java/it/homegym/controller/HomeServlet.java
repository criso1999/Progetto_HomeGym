package it.homegym.controller;

import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet({"/home", "/admin/home", "/staff/home"})
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Utente u = (Utente) req.getSession().getAttribute("user");
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        String ruolo = u.getRuolo();
        if ("PERSONALE".equals(ruolo)) {
            req.getRequestDispatcher("/WEB-INF/views/staff/home.jsp").forward(req, resp);
        } else if ("PROPRIETARIO".equals(ruolo)) {
            req.getRequestDispatcher("/WEB-INF/views/admin/home.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
        }
    }
}
