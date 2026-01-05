package it.homegym.controller;

import it.homegym.dao.PasswordResetDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/reset")
public class ResetPasswordServlet extends HttpServlet {

    private PasswordResetDAO tokenDao;
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        try {
            tokenDao = new PasswordResetDAO();
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // mostra form
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            Optional<PasswordResetDAO.TokenEntry> opt = tokenDao.findValidByToken(token);
            if (opt.isEmpty()) {
                req.setAttribute("error", "Token non valido o scaduto.");
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // effettua reset
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");

        if (token == null || token.isBlank() || password == null || password2 == null || !password.equals(password2)) {
            req.setAttribute("error", "Dati non validi o le password non coincidono.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
            return;
        }

        try {
            Optional<PasswordResetDAO.TokenEntry> opt = tokenDao.findValidByToken(token);
            if (opt.isEmpty()) {
                req.setAttribute("error", "Token non valido o scaduto.");
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }
            PasswordResetDAO.TokenEntry entry = opt.get();
            Utente u = utenteDao.findById(entry.userId);
            if (u == null) {
                req.setAttribute("error", "Utente non trovato.");
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }

            // hash e update password (UtenteDAO deve avere updatePassword)
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            boolean ok = utenteDao.updatePassword(u.getId(), hashed);
            if (!ok) {
                throw new ServletException("Impossibile aggiornare la password.");
            }

            // mark token used
            tokenDao.markUsed(token);

            // auto-login opzionale: effettuiamo redirect a login con messaggio
            HttpSession s = req.getSession();
            s.setAttribute("flashSuccess", "Password aggiornata. Effettua il login.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
