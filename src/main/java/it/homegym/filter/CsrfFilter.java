package it.homegym.filter;

import it.homegym.security.CsrfUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class CsrfFilter implements Filter {

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "DELETE");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  r = (HttpServletRequest) req;
        HttpServletResponse s = (HttpServletResponse) res;

        HttpSession session = r.getSession(true);
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", CsrfUtil.generateToken());
        }

        String method = r.getMethod().toUpperCase();
        // only protect state-changing operations
        if (PROTECTED_METHODS.contains(method)) {
            // allow X-CSRF-Token header (for AJAX) otherwise param _csrf
            String header = r.getHeader("X-CSRF-Token");
            String param  = r.getParameter("_csrf");
            String token = (header != null && !header.isEmpty()) ? header : param;

            String sessionToken = (String) session.getAttribute("csrfToken");
            if (sessionToken == null || token == null || !sessionToken.equals(token)) {
                s.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token mancante o non valido.");
                return;
            }
        }

        chain.doFilter(req, res);
    }
}
