package it.homegym.filter;

import it.homegym.model.Utente;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse s = (HttpServletResponse) res;
        String ctx = r.getContextPath();
        String path = r.getRequestURI().substring(ctx.length());

        // pubbliche
        if (path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")
                || path.equals("/") || path.equals("/index.jsp")
                || path.equals("/login") || path.equals("/register")
                || path.equals("/jnditest") || path.equals("/test") || path.equals("/logout")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = r.getSession(false);
        Utente user = (session != null) ? (Utente) session.getAttribute("user") : null;

        // se accesso non autenticato -> login
        if (user == null) {
            s.sendRedirect(ctx + "/login");
            return;
        }

        String ruolo = user.getRuolo() != null ? user.getRuolo() : "CLIENTE";

        // protezione /admin/* -> solo PROPRIETARIO
        if (path.startsWith("/admin/") || path.equals("/admin") ) {
            if (!"PROPRIETARIO".equals(ruolo)) {
                // 403 oppure redirect a home con messaggio
                s.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato: ruolo non autorizzato");
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        // protezione /staff/* -> PERSONALE o PROPRIETARIO
        if (path.startsWith("/staff/") || path.equals("/staff") ) {
            if (!"PERSONALE".equals(ruolo) && !"PROPRIETARIO".equals(ruolo)) {
                s.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato: ruolo non autorizzato");
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        // default: utente autenticato può accedere
        chain.doFilter(req, res);
    }
}
