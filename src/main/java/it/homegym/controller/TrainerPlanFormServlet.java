package it.homegym.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

@WebServlet("/staff/plans/form")
public class TrainerPlanFormServlet extends HttpServlet {
    private TrainingPlanDAO dao;

    @Override
    public void init() throws ServletException {
        try {
            dao = new TrainingPlanDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare TrainingPlanDAO", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        Utente user = s != null ? (Utente) s.getAttribute("user") : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"PERSONALE".equals(user.getRuolo()) && !"PROPRIETARIO".equals(user.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            TrainingPlan p = null;
            try {
                int planId = Integer.parseInt(id);
                p = dao.findById(planId);
                // se vuoi gestire il caso "non trovato" qui:
                if (p == null) {
                    req.getSession().setAttribute("flashError", "Piano non trovato.");
                    resp.sendRedirect(req.getContextPath() + "/staff/plans");
                    return;
                }
                req.setAttribute("plan", p);
            } catch (NumberFormatException nfe) {
                // id non numerico: redirect con messaggio
                log("ID piano non valido: " + id, nfe);
                req.getSession().setAttribute("flashError", "ID piano non valido.");
                resp.sendRedirect(req.getContextPath() + "/staff/plans");
                return;
            } catch (SQLException sqle) {
                // errore DB
                log("Errore caricamento piano id=" + id, sqle);
                throw new ServletException("Errore durante lettura piano", sqle);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/staff/plan-form.jsp").forward(req, resp);
    }
}
