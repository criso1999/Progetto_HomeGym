package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/client/plans")
public class ClientPlansServlet extends HttpServlet {

    private TrainingPlanDAO planDao;
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        try {
            planDao = new TrainingPlanDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare DAO", e);
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
        HttpSession session = requireSession(req, resp);
        if (session == null) return;

        Object uobj = session.getAttribute("user");
        if (!(uobj instanceof it.homegym.model.Utente)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Utente current = (Utente) uobj;

        try {
            List<TrainingPlanAssignment> assignments = planDao.listAssignmentsForUser(current.getId());
            Map<Integer, TrainingPlan> plansMap = new HashMap<>();
            Map<Integer, Utente> trainersMap = new HashMap<>();

            for (TrainingPlanAssignment a : assignments) {
                int pid = a.getPlanId();
                if (!plansMap.containsKey(pid)) {
                    TrainingPlan p = planDao.findById(pid);
                    plansMap.put(pid, p);
                }
                int tid = a.getTrainerId();
                if (tid > 0 && !trainersMap.containsKey(tid)) {
                    Utente t = utenteDao.findById(tid);
                    trainersMap.put(tid, t);
                }
            }

            req.setAttribute("assignments", assignments);
            req.setAttribute("plansMap", plansMap);
            req.setAttribute("trainersMap", trainersMap);

            req.getRequestDispatcher("/WEB-INF/views/client/plans.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Errore DB durante caricamento schede assegnate", e);
        }
    }
}
