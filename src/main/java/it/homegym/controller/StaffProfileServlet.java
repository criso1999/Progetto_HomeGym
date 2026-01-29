package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/profile")
public class StaffProfileServlet extends HttpServlet {

    private UtenteDAO utenteDAO;

    @Override
    public void init() throws ServletException {
        try {
            utenteDAO = new UtenteDAO();
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

    private boolean isStaff(Utente u) {
        if (u == null) return false;
        String r = u.getRuolo();
        return "PERSONALE".equals(r) || "PROPRIETARIO".equals(r);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente u = (Utente) s.getAttribute("user");
        if (!isStaff(u)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            Utente fresh = utenteDAO.findById(u.getId());
            if (fresh != null) {
                s.setAttribute("user", fresh);
                req.setAttribute("user", fresh);
            } else {
                req.setAttribute("user", u);
            }
            req.getRequestDispatcher("/WEB-INF/views/staff/profile.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // POST = update profile
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente sessionUser = (Utente) s.getAttribute("user");
        if (!isStaff(sessionUser)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        String action = req.getParameter("action");
        if (!"update".equals(action)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String email = req.getParameter("email");
        String telefono = req.getParameter("telefono");
        String bio = req.getParameter("bio");
        String newPwd = req.getParameter("password");
        String newPwd2 = req.getParameter("password2");

        if (nome == null || cognome == null || email == null ||
                nome.isBlank() || cognome.isBlank() || email.isBlank()) {
            s.setAttribute("flashError", "Compila tutti i campi obbligatori.");
            resp.sendRedirect(req.getContextPath() + "/staff/profile");
            return;
        }

        try {
            Utente u = utenteDAO.findById(sessionUser.getId());
            if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
            u.setNome(nome);
            u.setCognome(cognome);
            u.setEmail(email);
            u.setTelefono(telefono);
            u.setBio(bio);

            boolean ok = utenteDAO.update(u);
            if (!ok) {
                s.setAttribute("flashError", "Errore aggiornamento profilo.");
                resp.sendRedirect(req.getContextPath() + "/staff/profile");
                return;
            }

            // update password se richiesta
            if (newPwd != null && !newPwd.isBlank()) {
                if (!newPwd.equals(newPwd2)) {
                    s.setAttribute("flashError", "Le password non coincidono.");
                    resp.sendRedirect(req.getContextPath() + "/staff/profile");
                    return;
                }
                String hashed = BCrypt.hashpw(newPwd, BCrypt.gensalt(12));
                utenteDAO.updatePassword(u.getId(), hashed);
            }

            u.setPassword(null);
            s.setAttribute("user", u);
            s.setAttribute("flashSuccess", "Profilo aggiornato con successo.");
            resp.sendRedirect(req.getContextPath() + "/staff/profile");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
