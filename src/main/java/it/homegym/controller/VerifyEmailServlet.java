package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.dao.VerificationTokenDAO;
import it.homegym.dao.VerificationTokenDAO.TokenRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;

@WebServlet("/verify-email")
public class VerifyEmailServlet extends HttpServlet {

    private VerificationTokenDAO tokenDao;
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        tokenDao = new VerificationTokenDAO();
        utenteDao = new UtenteDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            req.getSession().setAttribute("flashError", "Token mancante.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            TokenRecord ti = tokenDao.findByToken(token);
            if (ti == null) {
                req.getSession().setAttribute("flashError", "Token non valido o già usato.");
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            if (ti.expiresAt != null && ti.expiresAt.toInstant().isBefore(Instant.now())) {
                // opzionale: cancella token scaduto
                tokenDao.deleteById(ti.id);
                req.getSession().setAttribute("flashError", "Token scaduto. Richiedi nuovo link.");
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            boolean ok = utenteDao.setEmailVerified(ti.userId);
            if (!ok) {
                req.getSession().setAttribute("flashError", "Impossibile verificare l'account. Contatta admin.");
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            // cancella il token (one-time use)
            tokenDao.deleteById(ti.id);

            req.getSession().setAttribute("flashSuccess", "Email verificata correttamente. Ora puoi effettuare il login.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (SQLException e) {
            throw new ServletException("Errore DB verifica email", e);
        }
    }
}
