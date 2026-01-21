package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/staff/plans")
public class StaffPlansServlet extends HttpServlet {

    private TrainingPlanDAO dao;

    @Override
    public void init() throws ServletException {
        try {
            dao = new TrainingPlanDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare TrainingPlanDAO", e);
        }
    }

    private HttpSession requireSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return s;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente user = (Utente) s.getAttribute("user");

        try {
            List<TrainingPlan> plans;
            if ("PROPRIETARIO".equals(user.getRuolo())) {
                // proprietario vede tutte le schede
                plans = dao.listAllPlans();
            } else {
                // il personale vede solo le proprie schede
                plans = dao.listPlansByTrainer(user.getId());
            }
            req.setAttribute("plans", plans);
            req.getRequestDispatcher("/WEB-INF/views/staff/plans.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Errore caricamento piani", e);
        }
    }
}
