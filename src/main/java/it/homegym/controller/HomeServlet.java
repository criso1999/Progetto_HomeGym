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
        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();

        if (session == null) {
            resp.sendRedirect(ctx + "/login");
            return;
        }

        Utente u = (Utente) session.getAttribute("user");
        if (u == null) {
            resp.sendRedirect(ctx + "/login");
            return;
        }

        String ruolo = u.getRuolo() != null ? u.getRuolo().trim().toUpperCase() : "CLIENTE";

        String redirect;
        switch (ruolo) {
            case "PROPRIETARIO":
                redirect = ctx + "/admin/home";
                break;
            case "PERSONALE":
                redirect = ctx + "/staff/home";
                break;
            default:
                // cliente o ruolo non riconosciuto -> area client
                redirect = ctx + "/client/home";
        }

        resp.sendRedirect(redirect);
    }
}
