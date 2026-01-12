package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.model.TrainingSession;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/client/sessions/action")
public class ClientSessionActionServlet extends HttpServlet {

    private SessionDAO sessionDAO;

    @Override
    public void init() throws ServletException {
        try {
            sessionDAO = new SessionDAO();
        } catch (Exception e) {
            throw new ServletException(e);
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente u = (Utente) s.getAttribute("user");

        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                String trainer = req.getParameter("trainer");
                String scheduled = req.getParameter("scheduled_at");
                String durationStr = req.getParameter("duration");
                String notes = req.getParameter("notes");

                if (trainer == null || trainer.isBlank() || scheduled == null || scheduled.isBlank()) {
                    req.setAttribute("error", "Trainer e data/ora obbligatori.");
                    req.getRequestDispatcher("/client/sessions/new").forward(req, resp);
                    return;
                }

                // scheduled is in format yyyy-MM-dd'T'HH:mm
                LocalDateTime ldt = LocalDateTime.parse(scheduled, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                Timestamp ts = Timestamp.valueOf(ldt);

                TrainingSession tsObj = new TrainingSession();
                tsObj.setUserId(u.getId());
                tsObj.setTrainer(trainer);
                tsObj.setWhen(ts);
                tsObj.setDurationMinutes(durationStr != null && !durationStr.isBlank() ? Integer.parseInt(durationStr) : 60);
                tsObj.setNotes(notes);

                sessionDAO.create(tsObj);
            } else if ("cancel".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                // optional: verify ownership
                TrainingSession existing = sessionDAO.findById(id);
                if (existing != null && existing.getUserId() != null && existing.getUserId().equals(u.getId())) {
                    sessionDAO.delete(id);
                } else {
                    // ignore or throw
                }
            }
            resp.sendRedirect(req.getContextPath() + "/client/sessions");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
