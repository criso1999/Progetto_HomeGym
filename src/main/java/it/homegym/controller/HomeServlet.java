package it.homegym.controller;

import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Utente u = (Utente) req.getSession().getAttribute("user");
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        String ruolo = u.getRuolo();
        if ("PERSONALE".equals(ruolo)) {
            req.getRequestDispatcher("/WEB-INF/views/homePersonale.jsp").forward(req, resp);
        } else if ("PROPRIETARIO".equals(ruolo)) {
            req.getRequestDispatcher("/WEB-INF/views/homeProprietario.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("/WEB-INF/views/homeCliente.jsp").forward(req, resp);
        }
    }
}
