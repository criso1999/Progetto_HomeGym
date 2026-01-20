package it.homegym.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.homegym.dao.PresenceDAO;
import it.homegym.model.Utente;

@WebServlet("/attendance/check")
public class AttendanceCheckServlet extends HttpServlet {
    private PresenceDAO presenceDao;

    @Override
    public void init() { presenceDao = new PresenceDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token mancante");
            return;
        }
        HttpSession sess = req.getSession(false);
        Utente user = sess != null ? (Utente) sess.getAttribute("user") : null;

        if (user != null && "CLIENTE".equals(user.getRuolo())) {
            // auto check-in per utente loggato
            try {
                boolean ok = presenceDao.validateAndConsumeToken(token, user.getId(), null);
                if (ok) {
                    sess.setAttribute("flashSuccess", "Check-in effettuato. Benvenuto!");
                    resp.sendRedirect(req.getContextPath() + "/client/profile");
                } else {
                    sess.setAttribute("flashError", "QR non valido o già utilizzato/scaduto.");
                    resp.sendRedirect(req.getContextPath() + "/client/profile");
                }
                return;
            } catch (SQLException e) { throw new ServletException(e); }
        }

        // se non loggato o staff: mostra form per scegliere cliente (staff)
        req.setAttribute("token", token);
        req.getRequestDispatcher("/WEB-INF/views/attendance/check-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String userIdStr = req.getParameter("userId"); // scelto dallo staff o inserito
        HttpSession sess = req.getSession(false);
        Utente scanner = sess != null ? (Utente) sess.getAttribute("user") : null;
        Integer scannerId = (scanner != null) ? scanner.getId() : null;

        if (userIdStr == null || userIdStr.isBlank()) {
            if (sess != null) sess.setAttribute("flashError", "Seleziona l'utente.");
            resp.sendRedirect(req.getContextPath() + "/attendance/check?token=" + token);
            return;
        }

        try {
            Integer userId = Integer.parseInt(userIdStr);
            boolean ok = presenceDao.validateAndConsumeToken(token, userId, scannerId);
            if (ok) {
                if (sess != null) sess.setAttribute("flashSuccess", "Check-in registrato per utente id=" + userId);
                resp.sendRedirect(req.getContextPath() + "/staff/sessions"); // o dove conviene
            } else {
                if (sess != null) sess.setAttribute("flashError", "Token non valido / già usato / scaduto.");
                resp.sendRedirect(req.getContextPath() + "/staff/sessions");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
