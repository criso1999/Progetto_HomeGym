package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/clients/action")
public class StaffClientActionServlet extends HttpServlet {
    private UtenteDAO dao;
    @Override
    public void init() throws ServletException {
        try { dao = new UtenteDAO(); } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        HttpSession session = req.getSession(false);
        Utente current = session != null ? (Utente) session.getAttribute("user") : null;

        try {
            if ("assign".equals(action)) {
                if (current == null || (!"PERSONALE".equals(current.getRuolo()) && !"PROPRIETARIO".equals(current.getRuolo()))) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                String existingUserId = req.getParameter("existingUserId");
                if (existingUserId == null || existingUserId.isBlank()) {
                    resp.sendRedirect(req.getContextPath() + "/staff/clients");
                    return;
                }
                int userId = Integer.parseInt(existingUserId);
                int trainerId = current.getId();
                // allow owner to override trainer selection via trainerId param
                if ("PROPRIETARIO".equals(current.getRuolo())) {
                    String t = req.getParameter("trainerId");
                    if (t != null && !t.isBlank()) {
                        try { trainerId = Integer.parseInt(t); } catch (NumberFormatException ignored) {}
                    }
                }
                dao.assignTrainerToUser(userId, trainerId);

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                Utente u = dao.findById(id);
                if (u != null) {
                    u.setNome(req.getParameter("nome"));
                    u.setCognome(req.getParameter("cognome"));
                    u.setEmail(req.getParameter("email"));

                    String trainerParam = req.getParameter("trainerId");
                    if (trainerParam != null && !trainerParam.isBlank()) {
                        try { u.setTrainerId(Integer.parseInt(trainerParam)); } catch (NumberFormatException ignored) { u.setTrainerId(null); }
                    } else {
                        u.setTrainerId(null);
                    }

                    dao.update(u);
                }

                String newPwd = req.getParameter("password");
                if (newPwd != null && !newPwd.isBlank()) {
                    dao.updatePassword(id, BCrypt.hashpw(newPwd, BCrypt.gensalt(12)));
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                dao.softDeleteById(id);
            }

            resp.sendRedirect(req.getContextPath() + "/staff/clients");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
