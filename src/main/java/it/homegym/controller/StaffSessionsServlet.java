package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.model.TrainingSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/staff/sessions")
public class StaffSessionsServlet extends HttpServlet {

    private SessionDAO sessionDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            sessionDAO = new SessionDAO();
        } catch (Exception ex) {
            throw new ServletException("Impossibile inizializzare SessionDAO", ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<TrainingSession> sessions = sessionDAO.listAll();
            req.setAttribute("sessions", sessions);
        } catch (Exception e) {
            // log e mostra messaggio di errore nella view
            e.printStackTrace();
            req.setAttribute("sessions", Collections.emptyList());
            req.setAttribute("error", "Errore caricamento sessioni. Controlla i log.");
        }

        req.getRequestDispatcher("/WEB-INF/views/staff/sessions.jsp").forward(req, resp);
    }
}
