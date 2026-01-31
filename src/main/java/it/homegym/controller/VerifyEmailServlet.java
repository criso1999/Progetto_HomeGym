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
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/verify-email")
public class VerifyEmailServlet extends HttpServlet {
    

    private VerificationTokenDAO tokenDao;
    private UtenteDAO utenteDao;

        @Override
        public void init() throws ServletException {
            tokenDao = new VerificationTokenDAO();
            utenteDao = new UtenteDAO();
        }

       private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(VerifyEmailServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        System.out.println("Received token: " + token);
        try {
            LOG.info("VerifyEmailServlet: called with token=" + token);

            boolean ok = tokenDao.verifyAndConsumeToken(token);
            LOG.info("VerifyEmailServlet: verifyAndConsumeToken returned " + ok + " for token=" + token);

            if (!ok) {
                req.getSession().setAttribute("flashError", "Token non valido o già usato.");
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            req.getSession().setAttribute("flashSuccess", "Email verificata correttamente. Ora puoi effettuare il login.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "VerifyEmailServlet: SQLException verifying token=" + token, e);
            throw new ServletException("Errore DB verifica email", e);
        }
    }

}
