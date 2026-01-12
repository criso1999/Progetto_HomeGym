package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.model.TrainingSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;

@WebServlet({"/staff/sessions/new","/staff/sessions/edit","/staff/sessions/view"})
public class StaffSessionFormServlet extends HttpServlet {
    private SessionDAO sessionDAO;

    @Override
    public void init() throws ServletException {
        try { sessionDAO = new SessionDAO(); } catch (Exception e) { throw new ServletException(e); }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
        // forward a staff/session-form.jsp (usa lo stesso form per new/edit)
        req.getRequestDispatcher("/WEB-INF/views/staff/session-form.jsp").forward(req, resp);
    }
}
