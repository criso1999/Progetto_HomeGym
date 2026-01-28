package it.homegym.controller;

import it.homegym.dao.SubscriptionDAO;
import it.homegym.model.Subscription;
import it.homegym.model.SubscriptionPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Admin UI per gestire piani e sottoscrizioni.
 */
@WebServlet("/staff/subscriptions")
public class StaffSubscriptionServlet extends HttpServlet {

    private SubscriptionDAO dao;

    @Override
    public void init() throws ServletException {
        super.init();
        dao = new SubscriptionDAO();
    }

    private HttpSession requireSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return s;
    }

    private boolean isStaff(Utente u) {
        if (u == null) return false;
        String r = u.getRuolo();
        return "PERSONALE".equals(r) || "PROPRIETARIO".equals(r);
    }

    private boolean isOwner(Utente u) {
        return u != null && "PROPRIETARIO".equals(u.getRuolo());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente user = (Utente) s.getAttribute("user");
        if (!isStaff(user)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            List<SubscriptionPlan> plans = dao.listAllPlans();
            List<Subscription> subs = dao.listAllSubscriptions();
            req.setAttribute("plans", plans);
            req.setAttribute("subscriptions", subs);
            req.getRequestDispatcher("/WEB-INF/views/staff/subscriptions.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Errore DB caricamento abbonamenti", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente user = (Utente) s.getAttribute("user");
        if (!isStaff(user)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = Optional.ofNullable(req.getParameter("action")).orElse("").trim();
        try {
            switch (action) {
                case "create-plan":
                    if (!isOwner(user)) { s.setAttribute("flashError", "Permesso negato."); break; }
                    handleCreatePlan(req, s);
                    break;
                case "update-plan":
                    if (!isOwner(user)) { s.setAttribute("flashError", "Permesso negato."); break; }
                    handleUpdatePlan(req, s);
                    break;
                case "delete-plan":
                    if (!isOwner(user)) { s.setAttribute("flashError", "Permesso negato."); break; }
                    handleDeletePlan(req, s);
                    break;
                case "activate":
                    handleActivateSubscription(req, s);
                    break;
                case "cancel":
                    handleCancelSubscription(req, s);
                    break;
                default:
                    s.setAttribute("flashError", "Azione non riconosciuta.");
            }
        } catch (SQLException e) {
            throw new ServletException("Errore DB durante operazione abbonamenti", e);
        }

        resp.sendRedirect(req.getContextPath() + "/staff/subscriptions");
    }

    // ----------------- helpers -----------------

    private int periodToDays(String period) {
        if (period == null) return 30;
        switch (period.trim().toUpperCase()) {
            case "MONTHLY":
            case "MENSILE":
            case "MESE":
                return 30;
            case "SEMESTRAL":
            case "SEMIANNUAL":
            case "SEMESTRALE":
            case "SEMI":
                // 6 mesi ~ 182 giorni (approssimazione)
                return 182;
            case "ANNUAL":
            case "ANNUALE":
            case "YEARLY":
            case "YEAR":
                return 365;
            default:
                // puoi permettere numeri personalizzati: "90" -> 90 giorni
                try {
                    return Integer.parseInt(period.trim());
                } catch (NumberFormatException nfe) {
                    return 30;
                }
        }
    }

    private void handleCreatePlan(HttpServletRequest req, HttpSession s) throws SQLException {
        String name = req.getParameter("name");
        String period = req.getParameter("period"); // es. MONTHLY, SEMESTRAL, ANNUAL oppure numero di giorni
        String priceCentsS = req.getParameter("priceCents");
        String currency = Optional.ofNullable(req.getParameter("currency")).orElse("EUR");
        String activeS = req.getParameter("active");

        if (name == null || name.isBlank() || period == null || period.isBlank() || priceCentsS == null || priceCentsS.isBlank()) {
            s.setAttribute("flashError", "Campi obbligatori mancanti per creare il piano.");
            return;
        }

        long priceCents;
        try {
            priceCents = Long.parseLong(priceCentsS);
        } catch (NumberFormatException nfe) {
            s.setAttribute("flashError", "Prezzo non valido.");
            return;
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        // code = short code identificativo del piano (es. MONTHLY)
        String code = period.trim().toUpperCase();
        plan.setCode(code);
        plan.setName(name.trim());
        plan.setDescription(Optional.ofNullable(req.getParameter("description")).orElse("").trim());
        plan.setDurationDays(periodToDays(period));
        plan.setPriceCents(priceCents);
        plan.setCurrency(currency.trim().toUpperCase());
        plan.setActive("1".equals(activeS) || "true".equalsIgnoreCase(activeS));

        int id = dao.createPlan(plan);
        if (id > 0) s.setAttribute("flashSuccess", "Piano creato (id: " + id + ").");
        else s.setAttribute("flashError", "Impossibile creare il piano.");
    }

    private void handleUpdatePlan(HttpServletRequest req, HttpSession s) throws SQLException {
        String idS = req.getParameter("planId");
        if (idS == null || idS.isBlank()) { s.setAttribute("flashError", "ID piano mancante."); return; }
        int id;
        try { id = Integer.parseInt(idS); } catch (NumberFormatException e){ s.setAttribute("flashError","ID piano non valido."); return; }

        SubscriptionPlan existing = dao.findPlanById(id);
        if (existing == null) { s.setAttribute("flashError","Piano non trovato."); return; }

        String name = req.getParameter("name");
        String period = req.getParameter("period");
        String priceCentsS = req.getParameter("priceCents");
        String currency = req.getParameter("currency");
        String activeS = req.getParameter("active");
        String description = req.getParameter("description");

        if (name != null && !name.isBlank()) existing.setName(name.trim());
        if (description != null) existing.setDescription(description.trim());
        if (period != null && !period.isBlank()) {
            existing.setCode(period.trim().toUpperCase());
            existing.setDurationDays(periodToDays(period));
        }
        if (priceCentsS != null && !priceCentsS.isBlank()) {
            try { existing.setPriceCents(Long.parseLong(priceCentsS)); } catch (NumberFormatException ignored) {}
        }
        if (currency != null && !currency.isBlank()) existing.setCurrency(currency.trim().toUpperCase());
        existing.setActive("1".equals(activeS) || "true".equalsIgnoreCase(activeS));

        boolean ok = dao.updatePlan(existing);
        if (ok) s.setAttribute("flashSuccess", "Piano aggiornato.");
        else s.setAttribute("flashError", "Errore aggiornamento piano.");
    }

    private void handleDeletePlan(HttpServletRequest req, HttpSession s) throws SQLException {
        String idS = req.getParameter("planId");
        if (idS == null || idS.isBlank()) { s.setAttribute("flashError", "ID piano mancante."); return; }
        int id;
        try { id = Integer.parseInt(idS); } catch (NumberFormatException e){ s.setAttribute("flashError","ID piano non valido."); return; }
        boolean ok = dao.deletePlan(id);
        if (ok) s.setAttribute("flashSuccess", "Piano rimosso.");
        else s.setAttribute("flashError", "Errore rimozione piano (potrebbe avere sottoscrizioni attive).");
    }

    private void handleActivateSubscription(HttpServletRequest req, HttpSession s) throws SQLException {
        String idS = req.getParameter("subscriptionId");
        if (idS == null || idS.isBlank()) { s.setAttribute("flashError", "ID sottoscrizione mancante."); return; }
        int id;
        try { id = Integer.parseInt(idS); } catch (NumberFormatException e){ s.setAttribute("flashError","ID sottoscrizione non valido."); return; }

        boolean ok = dao.activateSubscription(id);
        if (ok) s.setAttribute("flashSuccess", "Sottoscrizione attivata.");
        else s.setAttribute("flashError", "Errore attivazione sottoscrizione.");
    }

    private void handleCancelSubscription(HttpServletRequest req, HttpSession s) throws SQLException {
        String idS = req.getParameter("subscriptionId");
        if (idS == null || idS.isBlank()) { s.setAttribute("flashError", "ID sottoscrizione mancante."); return; }
        int id;
        try { id = Integer.parseInt(idS); } catch (NumberFormatException e){ s.setAttribute("flashError","ID sottoscrizione non valido."); return; }

        boolean ok = dao.cancelSubscription(id);
        if (ok) s.setAttribute("flashSuccess", "Sottoscrizione cancellata/annullata.");
        else s.setAttribute("flashError", "Errore cancellazione sottoscrizione.");
    }
}
