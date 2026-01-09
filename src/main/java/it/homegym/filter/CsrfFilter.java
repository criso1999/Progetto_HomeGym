package it.homegym.filter;

import it.homegym.security.CsrfUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Set;

/**
 * CSRF filter semplice:
 * - crea un token in sessione (se non presente)
 * - per ogni response espone il token tramite header X-CSRF-Token e cookie XSRF-TOKEN
 * - per metodi "state-changing" (POST/PUT/DELETE/PATCH) valida il token ricevuto
 *   (accetta X-CSRF-Token header oppure parametro _csrf)
 */
@WebFilter("/*")
public class CsrfFilter implements Filter {

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    // percorsi pubblici/risorse statiche per cui non vogliamo creare/validare CSRF
    private static final String[] PUBLIC_PREFIXES = new String[] {
            "/css", "/js", "/images", "/webjars", "/static", "/fonts"
    };

    private static final String[] PUBLIC_PATHS = new String[] {
            "/", "/index.jsp", "/login", "/register", "/forgot", "/reset", "/jnditest", "/test", "/adminer"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ctx = request.getContextPath();
        String path = request.getRequestURI().substring(ctx.length());

        // se è una risorsa statica o un path pubblico, non forziamo validazione CSRF
        if (isPublicPath(path)) {
            // però assicuriamoci che la sessione abbia il token (utile per mostrare form pubblici)
            ensureSessionToken(request);
            // esponi comunque il token via header/cookie (utile per fetch/AJAX client-side)
            exposeToken(request, response);
            chain.doFilter(request, response);
            return;
        }

        // garantiamo che la sessione abbia un token
        ensureSessionToken(request);
        // esponi token sulla response (header + cookie)
        exposeToken(request, response);

        String method = request.getMethod().toUpperCase();

        // solo per metodi che modificano stato facciamo la validazione
        if (PROTECTED_METHODS.contains(method)) {
            String header = request.getHeader("X-CSRF-Token");
            String param  = request.getParameter("_csrf");
            String token = (header != null && !header.isEmpty()) ? header : param;

            HttpSession session = request.getSession(false);
            String sessionToken = session != null ? (String) session.getAttribute("csrfToken") : null;

            if (sessionToken == null || token == null || !sessionToken.equals(token)) {
                // non validato -> 403
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token mancante o non valido.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void ensureSessionToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true); // crea se necessario
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", CsrfUtil.generateToken());
        }
    }

    private void exposeToken(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object t = session.getAttribute("csrfToken");
        if (!(t instanceof String)) return;
        String token = (String) t;

        // header utile per client JS che leggono X-CSRF-Token dalle response
        response.setHeader("X-CSRF-Token", token);

        // cookie leggibile da JS (HttpOnly=false) per semplici integrazioni front-end (fetch)
        Cookie c = new Cookie("XSRF-TOKEN", token);
        // imposta path al context root in modo che il cookie sia inviato su tutte le richieste
        String ctx = request.getContextPath();
        c.setPath((ctx == null || ctx.isEmpty()) ? "/" : ctx);
        c.setHttpOnly(false); // vogliamo che il client JS possa leggerlo
        c.setSecure(request.isSecure()); // mark secure se la connessione è TLS
        // SameSite può essere impostato tramite header se necessario - non tutti i container Java lo espongono:
        response.addCookie(c);

        // Se vuoi aggiungere anche un header SameSite (alcuni browser lo rispettano)
        // response.setHeader("Set-Cookie", String.format("XSRF-TOKEN=%s; Path=%s; %s; SameSite=Lax",
        //        token, c.getPath(), request.isSecure() ? "Secure" : ""));
    }

    private boolean isPublicPath(String path) {
        if (path == null) return true;
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p)) return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }
}
