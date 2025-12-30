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
        String path = r.getRequestURI().substring(r.getContextPath().length());

        // permit public urls
        if (path.startsWith("/css") || path.startsWith("/js") || path.equals("/login") || path.equals("/register") || path.equals("/jnditest") || path.equals("/test")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = r.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            s.sendRedirect(r.getContextPath() + "/login");
            return;
        }

        chain.doFilter(req, res);
    }
}
