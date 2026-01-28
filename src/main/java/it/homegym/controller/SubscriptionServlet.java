package it.homegym.controller;

import it.homegym.dao.SubscriptionDAO;
import it.homegym.dao.SubscriptionDAO; // assicurati di aggiungere import corretti
import it.homegym.dao.SubscriptionDAO;
import it.homegym.model.Subscription;
import it.homegym.model.SubscriptionPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet({"/subscriptions", "/subscriptions/subscribe", "/account/subscription"})
public class SubscriptionServlet extends HttpServlet {
    private SubscriptionDAO dao;

    @Override
    public void init() throws ServletException {
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

    // GET: lista piani o pagina account (action param)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = requireSession(req, resp);
        if (session == null) return;
        Utente user = (Utente) session.getAttribute("user");

        String path = req.getServletPath();
        try {
            if ("/subscriptions".equals(path) || "/subscriptions/subscribe".equals(path)) {
                List<SubscriptionPlan> plans = dao.listActivePlans();
                req.setAttribute("plans", plans);
                req.getRequestDispatcher("/WEB-INF/views/subscriptions/subscribe.jsp").forward(req, resp);
            } else { // /account/subscription
                Subscription sub = dao.getActiveSubscriptionForUser(user.getId());
                req.setAttribute("subscription", sub);
                req.getRequestDispatcher("/WEB-INF/views/account/subscription.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // POST: create pending subscription (then redirect to payment flow)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = requireSession(req, resp);
        if (session == null) return;
        Utente user = (Utente) session.getAttribute("user");

        String action = req.getParameter("action");
        if ("subscribe".equals(action)) {
            try {
                int planId = Integer.parseInt(req.getParameter("planId"));
                SubscriptionPlan plan = dao.findPlanById(planId);
                if (plan == null) {
                    session.setAttribute("flashError", "Piano non trovato.");
                    resp.sendRedirect(req.getContextPath() + "/subscriptions");
                    return;
                }
                // crea sottoscrizione PENDING; pagamento va gestito con provider (Stripe, ecc.)
                Subscription sub = new Subscription();
                sub.setUserId(user.getId());
                sub.setPlanId(plan.getId());
                sub.setStatus("PENDING");
                sub.setPriceCents(plan.getPriceCents());
                sub.setCurrency(plan.getCurrency());
                int subId = dao.createSubscription(sub);
                if (subId > 0) {
                    // Qui invii al provider di pagamento e redirezioni alla pagina checkout del provider
                    // es: crea sessione Stripe, passa subId come metadata -> quando webhook OK, chiami dao.activateSubscription(...)
                    session.setAttribute("flashSuccess", "Iniziata procedura di pagamento. Verrai reindirizzato al provider.");
                    // redirect placeholder (devi integrare provider)
                    resp.sendRedirect(req.getContextPath() + "/subscriptions");
                } else {
                    session.setAttribute("flashError", "Impossibile creare la sottoscrizione.");
                    resp.sendRedirect(req.getContextPath() + "/subscriptions");
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
