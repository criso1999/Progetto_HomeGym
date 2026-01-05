package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.dao.PasswordResetDAO;
import it.homegym.util.MailUtil;
import it.homegym.security.CsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


@WebServlet("/forgot")
public class ForgotPasswordServlet extends HttpServlet {

    private UtenteDAO utenteDao;
    private PasswordResetDAO tokenDao;

    @Override
    public void init() throws ServletException {
        try {
            utenteDao = new UtenteDAO();
            tokenDao = new PasswordResetDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Inserisci l'email.");
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        }

        try {
            // find user
            var u = utenteDao.findByEmail(email);
            if (u == null) {
                // per sicurezza non riveliamo che l'email non esiste
                req.setAttribute("info", "Se l'email è registrata riceverai un link per resettare la password.");
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }

            // crea token e salva (scade in 1 ora)
            String token = CsrfUtil.generateToken();
            Timestamp expires = Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS));
            tokenDao.createToken(u.getId(), token, expires);

            // costruisci link
            String base = System.getenv("APP_BASE_URL");
            if (base == null || base.isBlank()) {
                base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            } else if (!base.endsWith("/")) {
                base = base + req.getContextPath(); // assicurati che contenga context path
            }

            String resetLink = base + "/reset?token=" + token;

            // invia email (HTML)
            StringBuilder html = new StringBuilder();
            html.append("<p>Ciao ").append(u.getNome() != null ? u.getNome() : "").append(",</p>");
            html.append("<p>Hai richiesto il reset della password. Clicca il link qui sotto per reimpostarla. Il link scade in 1 ora.</p>");
            html.append("<p><a href=\"").append(resetLink).append("\">Reimposta password</a></p>");
            html.append("<p>Se non hai richiesto questa operazione ignora questa mail.</p>");
            try {
                MailUtil.send(u.getEmail(), "Reset password HomeGym", html.toString());
            } catch (Exception mex) {
                // log ma non rivelare all'utente
                mex.printStackTrace();
            }

            req.setAttribute("info", "Se l'email è registrata riceverai un link per resettare la password.");
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
