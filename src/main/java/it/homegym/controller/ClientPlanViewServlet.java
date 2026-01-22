package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.model.TrainingPlanVersion;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/client/plans/view")
public class ClientPlanViewServlet extends HttpServlet {

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

        String planIdS = req.getParameter("planId");
        String assignmentIdS = req.getParameter("assignmentId");

        if ((planIdS == null || planIdS.isBlank()) && (assignmentIdS == null || assignmentIdS.isBlank())) {
            session.setAttribute("flashError", "Parametri mancanti per visualizzare la scheda.");
            resp.sendRedirect(req.getContextPath() + "/client/plans");
            return;
        }

        try {
            Integer planId = null;
            Integer assignmentId = null;
            if (planIdS != null && !planIdS.isBlank()) planId = Integer.parseInt(planIdS);
            if (assignmentIdS != null && !assignmentIdS.isBlank()) assignmentId = Integer.parseInt(assignmentIdS);

            // recupera assegnazioni dell'utente e trova quella corretta
            List<TrainingPlanAssignment> assignments = planDao.listAssignmentsForUser(current.getId());
            TrainingPlanAssignment found = null;
            for (TrainingPlanAssignment a : assignments) {
                if (assignmentId != null && a.getId() == assignmentId) {
                    found = a;
                    break;
                }
                if (assignmentId == null && planId != null && a.getPlanId() == planId) {
                    // se assignmentId non fornito, prendiamo la prima assegnazione per questo plan
                    found = a;
                    break;
                }
            }

            if (found == null) {
                session.setAttribute("flashError", "Non sei autorizzato a visualizzare questa scheda o assegnazione inesistente.");
                resp.sendRedirect(req.getContextPath() + "/client/plans");
                return;
            }

            // controlla che planId corrisponda (se fornito)
            if (planId != null && found.getPlanId() != planId) {
                session.setAttribute("flashError", "Parametri non coerenti.");
                resp.sendRedirect(req.getContextPath() + "/client/plans");
                return;
            }

            // carica piano
            TrainingPlan plan = planDao.findById(found.getPlanId());
            if (plan == null) {
                session.setAttribute("flashError", "Scheda non trovata.");
                resp.sendRedirect(req.getContextPath() + "/client/plans");
                return;
            }

            // carica trainer info (null-safe)
            Utente trainer = null;
            try {
                Integer tid = null;
                try {
                    // supporta sia int primitivo sia Integer nel model
                    Object maybe = found.getTrainerId();
                    if (maybe instanceof Integer) tid = (Integer) maybe;
                    else if (maybe != null) tid = Integer.parseInt(maybe.toString());
                } catch (Exception ignored) {}
                if (tid != null && tid != 0) {
                    trainer = utenteDao.findById(tid);
                }
            } catch (SQLException se) {
                // se fallisce il caricamento del trainer non blocchiamo la view; manteniamo trainer=null
                log("Attenzione: impossibile caricare trainer per assignment " + found.getId(), se);
            }

            // carica storico versioni (opzionale)
            List<TrainingPlanVersion> history = planDao.getPlanHistory(found.getPlanId());

            req.setAttribute("plan", plan);
            req.setAttribute("assignment", found);
            req.setAttribute("trainer", trainer);
            req.setAttribute("history", history);

            req.getRequestDispatcher("/WEB-INF/views/client/plan-view.jsp").forward(req, resp);

        } catch (NumberFormatException nfe) {
            session.setAttribute("flashError", "Parametro numerico non valido.");
            resp.sendRedirect(req.getContextPath() + "/client/plans");
        } catch (SQLException sqle) {
            throw new ServletException("Errore DB caricamento scheda", sqle);
        }
    }
}
