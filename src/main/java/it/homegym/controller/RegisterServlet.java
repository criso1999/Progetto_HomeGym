package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.dao.VerificationTokenDAO;
import it.homegym.model.Utente;
import it.homegym.security.RecaptchaVerifier;
import it.homegym.util.EmailSender;
import org.mindrot.jbcrypt.BCrypt;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {


    private UtenteDAO dao;

    // Inizializza il DAO
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            dao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare UtenteDAO", e);
        }
    }

    // Gestione della richiesta GET (mostra il form di registrazione)
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

    // Escape HTML per prevenire XSS nelle email
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // Gestione della richiesta POST (elabora il form di registrazione)
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

            // --- crea token di verifica e invia email ---
            VerificationTokenDAO tokenDao = new VerificationTokenDAO();
            String token = java.util.UUID.randomUUID().toString();

            // scadenza 24 ore
            LocalDateTime exp = LocalDateTime.now().plusHours(24);
            Timestamp expiresAt = Timestamp.valueOf(exp);

            int tokenId = tokenDao.createToken(u.getId(), token, expiresAt);
            if (tokenId <= 0) {
                // se non riusciamo a salvare il token, informiamo
                req.setAttribute("error", "Impossibile generare token di verifica. Contatta l'amministratore.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }
            

            // costruisci URL verifica
            String scheme = req.getScheme(); // http/https
            String host = req.getServerName();
            int port = req.getServerPort();
            String context = req.getContextPath();
            String portPart = (port == 80 || port == 443) ? "" : ":" + port;
            String verifyUrl = scheme + "://" + host + portPart + context + "/verify-email?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8.name());

            String subject = "Verifica la tua email - HomeGym";
            String html = "<p>Ciao " + escapeHtml(u.getNome()) + ",</p>"
                    + "<p>Grazie per esserti registrato su HomeGym. Per completare la registrazione clicca il link sottostante:</p>"
                    + "<p><a href=\"" + verifyUrl + "\">Verifica la mia email</a></p>"
                    + "<p>Il link scadrà tra 24 ore.</p>"
                    + "<p>Se non hai effettuato questa richiesta ignora questa mail.</p>";

            try {
                EmailSender.sendVerificationEmail(u.getEmail(), subject, html);
            } catch (MessagingException | RuntimeException e) {
                e.printStackTrace();
                // opzionale: cancellare token o utente, qui semplicemente informiamo l'utente
                req.setAttribute("error", "Errore invio email di verifica. Contatta l'amministratore.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }

            // successo: mostra messaggio istruttivo
            req.setAttribute("info", "Registrazione completata. Controlla la tua email per il link di verifica.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);

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
