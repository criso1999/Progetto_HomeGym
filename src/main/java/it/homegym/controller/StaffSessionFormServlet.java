package it.homegym.controller;

import it.homegym.dao.SessionDAO;
import it.homegym.model.TrainingSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet({"/staff/sessions/new","/staff/sessions/edit","/staff/sessions/view"})
public class StaffSessionFormServlet extends HttpServlet {
    private SessionDAO sessionDAO;

    @Override
    public void init() throws ServletException {
        try { sessionDAO = new SessionDAO(); } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        if (id != null) {
            try {
                TrainingSession s = sessionDAO.findById(Integer.parseInt(id));
                req.setAttribute("session", s);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        // forward a staff/session-form.jsp (usa lo stesso form per new/edit)
        req.getRequestDispatcher("/WEB-INF/views/staff/session-form.jsp").forward(req, resp);
    }
}
