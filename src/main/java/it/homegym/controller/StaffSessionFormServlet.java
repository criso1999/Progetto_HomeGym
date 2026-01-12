package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.TrainingSession;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;

@WebServlet({"/staff/sessions/new", "/staff/sessions/edit", "/staff/sessions/view"})
public class StaffSessionFormServlet extends HttpServlet {

    private SessionDAO sessionDAO;
    private UtenteDAO utenteDAO;

    @Override
    public void init() throws ServletException {
        try {
            sessionDAO = new SessionDAO();
            utenteDAO = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare DAO", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // attributo per input datetime-local (sempre impostato, anche vuoto)
        req.setAttribute("scheduledAtInput", "");

        // carica lista trainer (PERSONALE) per la select (può essere vuota)
        try {
            List<Utente> trainers = utenteDAO.listByRole("PERSONALE");
            req.setAttribute("trainers", trainers);
        } catch (Exception e) {
            // log e fallback a lista vuota
            e.printStackTrace();
            req.setAttribute("trainers", java.util.Collections.emptyList());
        }

        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            try {
                TrainingSession s = sessionDAO.findById(Integer.parseInt(id));
                if (s != null) {
                    req.setAttribute("session", s);
                    // prepara valore per input datetime-local (yyyy-MM-dd'T'HH:mm)
                    Timestamp t = s.getWhen();
                    if (t != null) {
                        LocalDateTime ldt = t.toLocalDateTime();
                        String formatted = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                        req.setAttribute("scheduledAtInput", formatted);
                    }
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/staff/session-form.jsp").forward(req, resp);
    }
}
