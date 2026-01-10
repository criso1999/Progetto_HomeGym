package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import it.homegym.security.RecaptchaVerifier;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

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
        req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        // semplice validazione lato server, non esaustiva
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        // normalizza email
        if (email != null) email = email.trim().toLowerCase();

        // Basic form validation
        if (nome == null || cognome == null || email == null || password == null ||
                nome.isBlank() || cognome.isBlank() || email.isBlank() || password.isBlank()) {
            req.setAttribute("error", "Compila tutti i campi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        if (!isValidEmail(email)) {
            req.setAttribute("error", "Formato email non valido.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        // reCAPTCHA verification
        String recaptchaResp = req.getParameter("g-recaptcha-response");
        String secret = System.getenv("RECAPTCHA_SECRET");
        try {
            if (!RecaptchaVerifier.verify(secret, recaptchaResp, getClientIp(req))) {
                req.setAttribute("error", "Verifica reCAPTCHA fallita.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }
        } catch (IllegalStateException ise) {
            req.setAttribute("error", "reCAPTCHA non configurato. Contatta un amministratore.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Errore durante la verifica reCAPTCHA. Riprova più tardi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        try {
            // controllo preventivo: esiste già?
            if (dao.findByEmail(email) != null) {
                req.setAttribute("error", "Esiste già un account con questa email.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }

            // create user
            Utente u = new Utente();
            u.setNome(nome.trim());
            u.setCognome(cognome.trim());
            u.setEmail(email);
            u.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
            u.setRuolo("CLIENTE");
            boolean created = dao.create(u);
            if (!created) {
                // fallback generico
                req.setAttribute("error", "Errore creazione utente.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (SQLIntegrityConstraintViolationException sqlEx) {
            // se un altro processo ha creato la stessa email contestualmente
            req.setAttribute("error", "Esiste già un account con questa email.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
