package it.homegym.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple CSP filter — sets Content-Security-Policy-Report-Only header by default.
 * Adjust the policy string to your needs (and remove 'unsafe-inline' for production).
 */
@WebFilter("/*")
public class CspFilter implements Filter {

    // Policy in Report-Only mode for testing. Tweak as needed.
    private static final String CSP_REPORT_ONLY =
            "default-src 'self'; " +
            "script-src 'self' https://www.google.com https://www.gstatic.com 'unsafe-inline'; " +
            "frame-src https://www.google.com https://recaptcha.google.com; " +
            "img-src 'self' data: https://www.google.com https://www.gstatic.com; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "object-src 'none'; " +
            "base-uri 'self';";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* no-op */ }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // add CSP report-only header so we can observe violations without blocking
        response.setHeader("Content-Security-Policy-Report-Only", CSP_REPORT_ONLY);

        // optional: set a Report-To header or report-uri to collect violation reports
        // response.setHeader("Report-To", "...");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() { /* no-op */ }
}
