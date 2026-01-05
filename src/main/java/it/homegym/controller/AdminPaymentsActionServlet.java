package it.homegym.controller;

import it.homegym.dao.PaymentDAO;
import it.homegym.model.Payment;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@WebServlet("/admin/payments/action")
public class AdminPaymentsActionServlet extends HttpServlet {

    private PaymentDAO dao;
    private static final Set<String> ALLOWED = Set.of("PAID", "REFUNDED", "PENDING", "CANCELLED");

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            dao = new PaymentDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare PaymentDAO", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // sicurezza: utente loggato e ruolo (AuthFilter dovrebbe già garantirlo)
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Utente u = (Utente) session.getAttribute("user");
        if (u == null || !"PROPRIETARIO".equals(u.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || !"updateStatus".equals(action)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Azione non valida");
            return;
        }

        String idStr = req.getParameter("id");
        String newStatus = req.getParameter("status");
        if (idStr == null || newStatus == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri mancanti");
            return;
        }

        newStatus = newStatus.trim().toUpperCase();
        if (!ALLOWED.contains(newStatus)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Status non consentito");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID non valido");
            return;
        }

        try {
            boolean ok = dao.updateStatus(id, newStatus);
            session.setAttribute("flash", ok ? "Stato aggiornato con successo." : "Aggiornamento fallito (record non trovato).");
        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        // mantieni paginazione se fornita
        String page = req.getParameter("page");
        String pageSize = req.getParameter("pageSize");
        StringBuilder redirect = new StringBuilder(req.getContextPath() + "/admin/payments");
        boolean first = true;
        if (page != null) {
            redirect.append(first ? "?" : "&");
            redirect.append("page=").append(URLEncoder.encode(page, StandardCharsets.UTF_8));
            first = false;
        }
        if (pageSize != null) {
            redirect.append(first ? "?" : "&");
            redirect.append("pageSize=").append(URLEncoder.encode(pageSize, StandardCharsets.UTF_8));
        }

        resp.sendRedirect(redirect.toString());
    }
}
