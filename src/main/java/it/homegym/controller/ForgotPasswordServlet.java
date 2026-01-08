package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.dao.PasswordResetDAO;
import it.homegym.security.CsrfUtil;
import it.homegym.security.RateLimitService;
import it.homegym.security.RecaptchaVerifier;
import it.homegym.util.MailUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
        // fornisci la site key alla JSP
        req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
        req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
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
        String ip = getClientIp(req);

        // controlli base
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Inserisci l'email.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        }

        // verifica reCAPTCHA (se configurata)
        String recaptchaResp = req.getParameter("g-recaptcha-response");
        System.out.println("DEBUG: g-recaptcha-response = [" + recaptchaResp + "]");
        String secret = System.getenv("RECAPTCHA_SECRET");
        try {
            // se secret non impostato, RecaptchaVerifier.throw -> gestiamo come fallimento controllato
            boolean okRecaptcha = RecaptchaVerifier.verify(secret, recaptchaResp, req.getRemoteAddr());
            if (!okRecaptcha) {
                req.setAttribute("error", "Verifica reCAPTCHA fallita.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }
        } catch (IllegalStateException ise) {
            // reCAPTCHA non configurato: preferisco fallire e chiedere di configurare (più sicuro)
            ise.printStackTrace();
            req.setAttribute("error", "reCAPTCHA non configurato correttamente. Contatta un amministratore.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        } catch (Exception e) {
            // problemi di rete ecc: log e messaggio generico all'utente
            e.printStackTrace();
            req.setAttribute("error", "Errore durante la verifica reCAPTCHA. Riprova più tardi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        }

        // Rate limiting: per IP
        if (!RateLimitService.ipLimiter.allow(ip)) {
            resp.setStatus(429);
            req.setAttribute("error", "Troppe richieste dal tuo IP. Riprova più tardi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        }

        // Rate limiting: per email (riduce enumeration attack e spam)
        String emailKey = email.toLowerCase();
        if (!RateLimitService.emailLimiter.allow(emailKey)) {
            resp.setStatus(429);
            req.setAttribute("error", "Troppe richieste per questa email. Riprova più tardi.");
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            return;
        }

        // Flow normale: crea token e invia mail (se l'email esiste)
        try {
            var u = utenteDao.findByEmail(email);
            if (u == null) {
                // per sicurezza: non rivelare che l'email non esiste
                req.setAttribute("info", "Se l'email è registrata riceverai un link per resettare la password.");
                req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
                req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                return;
            }

            // crea token e salva (scade in 1 ora)
            String token = CsrfUtil.generateToken();
            Timestamp expires = Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS));
            tokenDao.createToken(u.getId(), token, expires);

            // costruisci link base (usa APP_BASE_URL se impostata)
            String base = System.getenv("APP_BASE_URL");
            if (base == null || base.isBlank()) {
                base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            } else {
                // assicurati include context path senza doppio slash
                if (!base.endsWith(req.getContextPath())) {
                    base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
                    base = base + req.getContextPath();
                }
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
            req.setAttribute("recaptchaSiteKey", System.getenv("RECAPTCHA_SITE_KEY"));
            req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
