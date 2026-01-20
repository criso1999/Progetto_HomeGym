package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.model.TrainingSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/staff/sessions/action")
public class StaffSessionsActionServlet extends HttpServlet {

    private SessionDAO sessionDAO;

    @Override
    public void init() throws ServletException {
        try {
            sessionDAO = new SessionDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare SessionDAO", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                TrainingSession s = fromRequest(req);
                sessionDAO.create(s);
            } else if ("update".equals(action)) {
                TrainingSession s = fromRequest(req);
                s.setId(Integer.parseInt(req.getParameter("id")));
                sessionDAO.update(s);
            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                sessionDAO.delete(id);
            }
            resp.sendRedirect(req.getContextPath() + "/staff/sessions");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private TrainingSession fromRequest(HttpServletRequest req) {
        TrainingSession s = new TrainingSession();
        String userId = req.getParameter("userId");
        if (userId != null && !userId.isBlank()) {
            try { s.setUserId(Integer.parseInt(userId)); } catch (NumberFormatException ignored) {}
        }
        s.setTrainer(req.getParameter("trainer"));

        String scheduled = req.getParameter("scheduled_at"); // expected yyyy-MM-dd'T'HH:mm
        if (scheduled != null && !scheduled.isBlank()) {
            LocalDateTime ldt = LocalDateTime.parse(scheduled, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            s.setWhen(new java.util.Date(Timestamp.valueOf(ldt).getTime()));
        }

        String duration = req.getParameter("duration");
        if (duration != null && !duration.isBlank()) {
            try { s.setDurationMinutes(Integer.parseInt(duration)); } catch (NumberFormatException ignored) { s.setDurationMinutes(null); }
        } else {
            s.setDurationMinutes(null);
        }

        s.setNotes(req.getParameter("notes"));
        return s;
    }
}
