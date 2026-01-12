package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/client/profile")
public class ClientProfileServlet extends HttpServlet {

    private UtenteDAO utenteDAO;

    @Override
    public void init() throws ServletException {
        try {
            utenteDAO = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private HttpSession requireSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        return s;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente u = (Utente) s.getAttribute("user");
        // assicura dati freschi (opzionale)
        try {
            Utente fresh = utenteDAO.findById(u.getId());
            if (fresh != null) u = fresh;
        } catch (Exception ignored) {}
        req.setAttribute("user", u);
        req.getRequestDispatcher("/WEB-INF/views/client/profile.jsp").forward(req, resp);
    }

    // aggiorna profilo
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;
        Utente sessionUser = (Utente) s.getAttribute("user");

        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String email = req.getParameter("email");
        String newPwd = req.getParameter("password");
        String newPwd2 = req.getParameter("password2");

        if (nome == null || cognome == null || email == null ||
                nome.isBlank() || cognome.isBlank() || email.isBlank()) {
            req.setAttribute("error", "Compila tutti i campi obbligatori.");
            req.getRequestDispatcher("/WEB-INF/views/client/profile.jsp").forward(req, resp);
            return;
        }

        try {
            Utente u = utenteDAO.findById(sessionUser.getId());
            if (u == null) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            u.setNome(nome);
            u.setCognome(cognome);
            u.setEmail(email);
            boolean ok = utenteDAO.update(u);
            if (!ok) {
                req.setAttribute("error", "Errore aggiornamento profilo.");
                req.getRequestDispatcher("/WEB-INF/views/client/profile.jsp").forward(req, resp);
                return;
            }

            // update password se richiesta
            if (newPwd != null && !newPwd.isBlank()) {
                if (!newPwd.equals(newPwd2)) {
                    req.setAttribute("error", "Le password non coincidono.");
                    req.getRequestDispatcher("/WEB-INF/views/client/profile.jsp").forward(req, resp);
                    return;
                }
                String hashed = BCrypt.hashpw(newPwd, BCrypt.gensalt(12));
                utenteDAO.updatePassword(u.getId(), hashed);
            }

            // aggiorna session user (non memorizzare password)
            u.setPassword(null);
            s.setAttribute("user", u);

            req.setAttribute("info", "Profilo aggiornato con successo.");
            req.setAttribute("user", u);
            req.getRequestDispatcher("/WEB-INF/views/client/profile.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
