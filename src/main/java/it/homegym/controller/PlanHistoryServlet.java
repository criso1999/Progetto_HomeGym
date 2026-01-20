package it.homegym.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlanVersion;

@WebServlet("/staff/plans/history")
public class PlanHistoryServlet extends HttpServlet {
    private TrainingPlanDAO dao;
    @Override public void init() { dao = new TrainingPlanDAO(); }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("planId");
        if (id == null) { resp.sendRedirect(req.getContextPath()+"/staff/plans"); return; }
        try {
            List<TrainingPlanVersion> hist = dao.getPlanHistory(Integer.parseInt(id));
            req.setAttribute("history", hist);
            req.getRequestDispatcher("/WEB-INF/views/staff/plan-history.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}

