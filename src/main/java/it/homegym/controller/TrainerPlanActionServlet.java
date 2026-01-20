package it.homegym.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

@WebServlet("/staff/plans/action")
public class TrainerPlanActionServlet extends HttpServlet {
    private TrainingPlanDAO dao;
    @Override public void init() { dao = new TrainingPlanDAO(); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        Utente user = s != null ? (Utente) s.getAttribute("user") : null;
        if (user==null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                TrainingPlan p = new TrainingPlan();
                p.setTitle(req.getParameter("title"));
                p.setDescription(req.getParameter("description"));
                p.setContent(req.getParameter("content"));
                p.setCreatedBy(user.getId());
                dao.createPlan(p);
            } else if ("update".equals(action)) {
                TrainingPlan p = dao.findById(Integer.parseInt(req.getParameter("id")));
                p.setTitle(req.getParameter("title"));
                p.setDescription(req.getParameter("description"));
                p.setContent(req.getParameter("content"));
                dao.updatePlan(p, user.getId());
            } else if ("assign".equals(action)) {
                int planId = Integer.parseInt(req.getParameter("planId"));
                int userId = Integer.parseInt(req.getParameter("userId"));
                String notes = req.getParameter("notes");
                dao.assignPlanToUser(planId, userId, user.getId(), notes);
            } else if ("delete".equals(action)) {
                int planId = Integer.parseInt(req.getParameter("id"));
                dao.softDeletePlan(planId);
            }
            resp.sendRedirect(req.getContextPath()+"/staff/plans");
        } catch (Exception e) { throw new ServletException(e); }
    }
}

