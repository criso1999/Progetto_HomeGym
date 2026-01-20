package it.homegym.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.model.Utente;

@WebServlet("/client/plans")
public class ClientPlansServlet extends HttpServlet {
    private TrainingPlanDAO dao;
    @Override public void init() { dao = new TrainingPlanDAO(); }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        Utente user = s != null ? (Utente) s.getAttribute("user") : null;
        if (user == null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }
        try {
            List<TrainingPlanAssignment> assignments = dao.listAssignmentsForUser(user.getId());
            req.setAttribute("assignments", assignments);
            req.getRequestDispatcher("/WEB-INF/views/client/plans-list.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}

