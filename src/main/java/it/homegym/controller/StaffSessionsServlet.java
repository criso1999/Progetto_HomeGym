package it.homegym.controller;

import it.homegym.model.TrainingSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/staff/sessions")
public class StaffSessionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO: sostituire con chiamata al DB tramite SessionDAO
        List<TrainingSession> sessions = new ArrayList<>();

        // esempio demo (comentare/rimuovere in produzione)
        TrainingSession s1 = new TrainingSession();
        s1.setId(1);
        s1.setUserId(42);
        s1.setUserName("Mario Rossi");
        s1.setTrainer("Luca");
        s1.setWhen(Timestamp.from(Instant.now().plusSeconds(3600)));
        s1.setDurationMinutes(60);
        s1.setNotes("Valutazione iniziale");
        sessions.add(s1);

        // imposta attributi per la JSP
        req.setAttribute("sessions", sessions);

        // forward alla view sotto /WEB-INF
        req.getRequestDispatcher("/WEB-INF/views/staff/sessions.jsp").forward(req, resp);
    }
}
