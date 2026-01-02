package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

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
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String pwd = req.getParameter("password");

        if (email == null || pwd == null || email.isEmpty() || pwd.isEmpty()) {
            req.setAttribute("error", "Email e password richieste.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        try {
            Utente u = dao.findByEmail(email);
            if (u == null || u.getPassword() == null || !BCrypt.checkpw(pwd, u.getPassword())) {
                req.setAttribute("error", "Credenziali errate.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            HttpSession session = req.getSession(true);
            u.setPassword(null); // non memorizzare la password in sessione
            session.setAttribute("user", u);
            session.setMaxInactiveInterval(30 * 60); // 30 minuti
            // dopo aver impostato session.setAttribute("user", u);
            String ctx = req.getContextPath();
            resp.sendRedirect(ctx + "/home");
                } catch (Exception ex) {
                    throw new ServletException(ex);
                }
            }
            }
