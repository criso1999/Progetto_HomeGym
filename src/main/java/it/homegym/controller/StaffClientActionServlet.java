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
        // CSRF / auth filters già presenti a livello di app
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                Utente u = new Utente();
                u.setNome(req.getParameter("nome"));
                u.setCognome(req.getParameter("cognome"));
                u.setEmail(req.getParameter("email"));
                String pwd = req.getParameter("password");
                u.setPassword(BCrypt.hashpw(pwd, BCrypt.gensalt(12)));
                u.setRuolo("CLIENTE");

                // trainer assignment (nullable)
                String trainerParam = req.getParameter("trainerId");
                if (trainerParam != null && !trainerParam.isBlank()) {
                    try { u.setTrainerId(Integer.parseInt(trainerParam)); } catch (NumberFormatException ignored) { u.setTrainerId(null); }
                } else {
                    u.setTrainerId(null);
                }

                dao.create(u);

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                Utente u = dao.findById(id);
                if (u != null) {
                    u.setNome(req.getParameter("nome"));
                    u.setCognome(req.getParameter("cognome"));
                    u.setEmail(req.getParameter("email"));

                    // trainer assignment (nullable)
                    String trainerParam = req.getParameter("trainerId");
                    if (trainerParam != null && !trainerParam.isBlank()) {
                        try { u.setTrainerId(Integer.parseInt(trainerParam)); } catch (NumberFormatException ignored) { u.setTrainerId(null); }
                    } else {
                        u.setTrainerId(null);
                    }

                    dao.update(u);
                }

                // update password if provided
                String newPwd = req.getParameter("password");
                if (newPwd != null && !newPwd.isBlank()) {
                    dao.updatePassword(id, BCrypt.hashpw(newPwd, BCrypt.gensalt(12)));
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                // soft delete: non cancelliamo la riga, la markiamo come deleted = 1
                dao.softDeleteById(id);
            }

            resp.sendRedirect(req.getContextPath() + "/staff/clients");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
