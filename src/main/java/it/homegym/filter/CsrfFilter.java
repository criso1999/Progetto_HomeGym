package it.homegym.filter;

import it.homegym.security.CsrfUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;

/**
 * CSRF filter aggiornato:
 * - crea un token in sessione (se non presente)
 * - espone il token via header X-CSRF-Token e cookie XSRF-TOKEN
 * - per metodi "protetti" (POST/PUT/DELETE/PATCH) valida il token ricevuto
 *   (accetta X-CSRF-Token header, parametro _csrf, cookie XSRF-TOKEN, o part _csrf in multipart)
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
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ctx = request.getContextPath();
        String path = request.getRequestURI().substring(ctx.length());

        // se è una risorsa statica o un path pubblico, non forziamo validazione CSRF
        if (isPublicPath(path)) {
            ensureSessionToken(request);
            exposeToken(request, response);
            chain.doFilter(request, response);
            return;
        }

        // garantiamo che la sessione abbia un token
        ensureSessionToken(request);
        // esponi token sulla response (header + cookie)
        exposeToken(request, response);

        String method = request.getMethod().toUpperCase();

        if (PROTECTED_METHODS.contains(method)) {
            String sessionToken = getSessionToken(request);
            if (sessionToken == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token mancante o non valido.");
                return;
            }

            // 1) header
            String header = request.getHeader("X-CSRF-Token");
            if (header != null && header.equals(sessionToken)) {
                chain.doFilter(request, response);
                return;
            }

            // 2) normal param (works for non-multipart)
            String param = request.getParameter("_csrf");
            if (param != null && param.equals(sessionToken)) {
                chain.doFilter(request, response);
                return;
            }

            // 3) cookie fallback (XSRF-TOKEN)
            String cookieVal = getCookieValue(request, "XSRF-TOKEN");
            if (cookieVal != null && cookieVal.equals(sessionToken)) {
                chain.doFilter(request, response);
                return;
            }

            // 4) if multipart, try reading part named "_csrf"
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
                try {
                    // try getPart first (container may or may not support)
                    Part csrfPart = null;
                    try {
                        csrfPart = request.getPart("_csrf");
                    } catch (IllegalStateException | ServletException | IOException ignored) {
                        // fallback to scanning parts
                    }

                    if (csrfPart == null) {
                        try {
                            Collection<Part> parts = request.getParts();
                            if (parts != null) {
                                for (Part p : parts) {
                                    if ("_csrf".equals(p.getName())) {
                                        csrfPart = p;
                                        break;
                                    }
                                }
                            }
                        } catch (IllegalStateException | ServletException | IOException ex) {
                            // non possiamo leggere le parts: log e proseguire al rifiuto
                            log("CsrfFilter: impossibile leggere multipart parts: " + ex.getMessage());
                        }
                    }

                    if (csrfPart != null) {
                        String partVal = readPartAsString(csrfPart);
                        if (partVal != null && partVal.equals(sessionToken)) {
                            chain.doFilter(request, response);
                            return;
                        }
                    }
                } catch (Exception e) {
                    // qualsiasi errore qui non deve esporre la request: loggare e rifiutare
                    log("CsrfFilter: errore checking multipart _csrf: " + e.getMessage());
                }
            }

            // se arriviamo qui -> nessuna verifica ha passato => 403
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token mancante o non valido.");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getSessionToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (String) session.getAttribute("csrfToken") : null;
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
        String ctx = request.getContextPath();
        c.setPath((ctx == null || ctx.isEmpty()) ? "/" : ctx);
        c.setHttpOnly(false);
        c.setSecure(request.isSecure());
        response.addCookie(c);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static String readPartAsString(Part p) {
        try (InputStream is = p.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
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

    @Override
    public void destroy() {
        // no-op
    }

    private void log(String msg) {
        System.err.println(msg);
    }
}
