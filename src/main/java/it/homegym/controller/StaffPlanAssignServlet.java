package it.homegym.controller;

import it.homegym.dao.TrainingPlanDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.TrainingPlan;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/staff/plans/assign")
public class StaffPlanAssignServlet extends HttpServlet {

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
        Utente current = (Utente) session.getAttribute("user");
        if (!"PERSONALE".equals(current.getRuolo()) && !"PROPRIETARIO".equals(current.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String planId = req.getParameter("planId");
        if (planId == null || planId.isBlank()) {
            session.setAttribute("flashError", "ID piano mancante.");
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
            return;
        }

        try {
            TrainingPlan plan = planDao.findById(Integer.parseInt(planId));
            if (plan == null) {
                session.setAttribute("flashError", "Piano non trovato.");
                resp.sendRedirect(req.getContextPath() + "/staff/plans");
                return;
            }

            List<Utente> clients;
            if ("PROPRIETARIO".equals(current.getRuolo())) {
                // proprietario può scegliere qualsiasi cliente non deleted
                clients = utenteDao.listByRole("CLIENTE");
                // anche fornisco la lista dei trainers per il select opzionale nel JSP
                List<Utente> trainers = utenteDao.listByRole("PERSONALE");
                req.setAttribute("trainers", trainers);
            } else {
                // trainer può scegliere solo i propri clienti (non deleted)
                clients = utenteDao.listClientsByTrainer(current.getId());
            }

            req.setAttribute("plan", plan);
            req.setAttribute("clients", clients);
            req.getRequestDispatcher("/WEB-INF/views/staff/plan-assign.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Errore DB durante caricamento dati", e);
        } catch (NumberFormatException nfe) {
            session.setAttribute("flashError", "ID piano non valido.");
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
        }
    }

    // esegue assign
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = requireSession(req, resp);
        if (session == null) return;
        Utente current = (Utente) session.getAttribute("user");
        if (!"PERSONALE".equals(current.getRuolo()) && !"PROPRIETARIO".equals(current.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String planIdS = req.getParameter("planId");
        String userIdS = req.getParameter("userId");
        String trainerIdS = req.getParameter("trainerId"); // optional for owner to set trainer explicitly
        String notes = req.getParameter("notes");

        if (planIdS == null || userIdS == null || planIdS.isBlank() || userIdS.isBlank()) {
            session.setAttribute("flashError", "Campi obbligatori mancanti.");
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
            return;
        }

        try {
            int planId = Integer.parseInt(planIdS);
            int userId = Integer.parseInt(userIdS);

            int trainerId = current.getId();
            if ("PROPRIETARIO".equals(current.getRuolo()) && trainerIdS != null && !trainerIdS.isBlank()) {
                try { trainerId = Integer.parseInt(trainerIdS); } catch (NumberFormatException ignored) {}
            }

            // Security: if current is trainer ensure the target user is among their clients
            if ("PERSONALE".equals(current.getRuolo())) {
                List<Integer> clientIds = utenteDao.listClientIdsByTrainer(current.getId());
                if (clientIds == null || !clientIds.contains(userId)) {
                    session.setAttribute("flashError", "Il cliente selezionato non è assegnato a te.");
                    resp.sendRedirect(req.getContextPath() + "/staff/plans");
                    return;
                }
            }

            int assignmentId = planDao.assignPlanToUser(planId, userId, trainerId, notes);
            if (assignmentId > 0) {
                session.setAttribute("flashSuccess", "Scheda assegnata al cliente (id assegnazione: " + assignmentId + ").");
            } else {
                session.setAttribute("flashError", "Impossibile assegnare la scheda.");
            }
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
        } catch (NumberFormatException ex) {
            session.setAttribute("flashError", "Parametri numerici non validi.");
            resp.sendRedirect(req.getContextPath() + "/staff/plans");
        } catch (SQLException e) {
            throw new ServletException("Errore DB durante assegnazione", e);
        }
    }
}
