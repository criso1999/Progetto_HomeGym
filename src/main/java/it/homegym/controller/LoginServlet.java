package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import it.homegym.security.RecaptchaVerifier;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // passa la site key alla JSP
        req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String pwd = req.getParameter("password");

        // Basic validation
        if (email == null || pwd == null || email.isEmpty() || pwd.isEmpty()) {
            req.setAttribute("error", "Email e password richieste.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        // reCAPTCHA verification (server-side)
        String recaptchaResp = req.getParameter("g-recaptcha-response");
        String secret = System.getenv("RECAPTCHA_SECRET");
        try {
            if (!RecaptchaVerifier.verify(secret, recaptchaResp, getClientIp(req))) {
                req.setAttribute("error", "Verifica reCAPTCHA fallita.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }
        } catch (IllegalStateException ise) {
            // reCAPTCHA non configurato: mostra messaggio amministrativo (o fallback)
            req.setAttribute("error", "reCAPTCHA non configurato correttamente. Contatta un amministratore.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Errore durante la verifica reCAPTCHA. Riprova più tardi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        // Authenticate user
        try {
            Utente u = dao.findByEmail(email);
            if (u == null || u.getPassword() == null || !BCrypt.checkpw(pwd, u.getPassword())) {
                req.setAttribute("error", "Credenziali errate.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            HttpSession session = req.getSession(true);
            u.setPassword(null);
            session.setAttribute("user", u);
            session.setMaxInactiveInterval(30 * 60);

            resp.sendRedirect(req.getContextPath() + "/home");
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
