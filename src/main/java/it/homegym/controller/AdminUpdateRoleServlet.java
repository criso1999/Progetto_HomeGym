package it.homegym.controller;

import it.homegym.dao.UtenteDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/users/action")
public class AdminUpdateRoleServlet extends HttpServlet {

    private UtenteDAO dao;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            dao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare UtenteDAO", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        String idStr = req.getParameter("id");
        if (idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            if ("changeRole".equals(action)) {
                String newRole = req.getParameter("role");
                if (newRole != null && (newRole.equals("CLIENTE") || newRole.equals("PERSONALE") || newRole.equals("PROPRIETARIO"))) {
                    dao.updateRole(id, newRole);
                }
            } else if ("delete".equals(action)) {
                dao.deleteById(id);
            }
            resp.sendRedirect(req.getContextPath() + "/admin/users");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
