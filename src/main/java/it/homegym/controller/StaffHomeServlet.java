package it.homegym.controller;

import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/home")
public class StaffHomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Utente u = (Utente) s.getAttribute("user");
        req.setAttribute("user", u);

        req.getRequestDispatcher("/WEB-INF/views/staff/home.jsp").forward(req, resp);
    }
}
