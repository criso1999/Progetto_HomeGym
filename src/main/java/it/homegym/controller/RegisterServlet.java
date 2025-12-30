package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

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
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String ruolo = req.getParameter("ruolo") != null ? req.getParameter("ruolo") : "CLIENTE";

        if (nome == null || email == null || password == null || nome.isEmpty() || email.isEmpty() || password.isEmpty()) {
            req.setAttribute("error", "Compila tutti i campi richiesti.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        try {
            if (dao.findByEmail(email) != null) {
                req.setAttribute("error", "Email già registrata.");
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
                return;
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            Utente u = new Utente();
            u.setNome(nome);
            u.setCognome(cognome);
            u.setEmail(email);
            u.setPassword(hashed);
            u.setRuolo(ruolo);

            if (dao.create(u)) {
                // auto-login (senza password nella sessione)
                u.setPassword(null);
                HttpSession session = req.getSession(true);
                session.setAttribute("user", u);
                resp.sendRedirect(req.getContextPath() + "/home");
            } else {
                req.setAttribute("error", "Errore nella registrazione.");
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
