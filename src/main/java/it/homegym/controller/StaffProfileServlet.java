package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestisce GET/POST per /staff/profile
 * GET  -> forward a /WEB-INF/views/staff/profile.jsp (la JSP che hai già creato)
 * POST -> aggiorna dati profilo e aggiorna sessionScope.user
 *
 * Nota: UtenteDAO deve avere almeno:
 *   - Utente findById(int id) throws SQLException;
 *   - boolean update(Utente u) throws SQLException;
 * Adatta i nomi/metodi se la tua DAO usa altre firme.
 */
@WebServlet("/staff/profile")
public class StaffProfileServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(StaffProfileServlet.class.getName());
    private UtenteDAO utenteDao;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            utenteDao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare UtenteDAO", e);
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

    private boolean isStaff(Utente u) {
        if (u == null) return false;
        String r = u.getRuolo();
        return "PERSONALE".equals(r) || "PROPRIETARIO".equals(r);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;

        Utente sessionUser = (Utente) s.getAttribute("user");
        if (!isStaff(sessionUser)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Ricarica utente dal DB per mostrare dati aggiornati (se possibile)
        try {
            Utente fresh = utenteDao.findById(sessionUser.getId());
            if (fresh != null) {
                s.setAttribute("user", fresh); // aggiorna sessione
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Impossibile ricaricare utente id=" + sessionUser.getId() + ": " + e.getMessage(), e);
            // non blocchiamo la visualizzazione: user session rimane valido
        }

        req.getRequestDispatcher("/WEB-INF/views/staff/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = requireSession(req, resp);
        if (s == null) return;

        Utente sessionUser = (Utente) s.getAttribute("user");
        if (!isStaff(sessionUser)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        if (action == null) action = "";

        if (!"update".equals(action)) {
            s.setAttribute("flashError", "Azione non riconosciuta.");
            resp.sendRedirect(req.getContextPath() + "/staff/profile");
            return;
        }

        // Leggi parametri
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String email = req.getParameter("email");
        String telefono = req.getParameter("telefono");
        String bio = req.getParameter("bio");

        // Validazioni base
        if (email == null || email.isBlank()) {
            s.setAttribute("flashError", "L'email non può essere vuota.");
            resp.sendRedirect(req.getContextPath() + "/staff/profile");
            return;
        }

        // Applica aggiornamenti all'oggetto utente (sessione)
        Utente toUpdate = new Utente();
        toUpdate.setId(sessionUser.getId());
        // conserva campi non esposti nel form (se Utente ha altri campi)
        // oppure ricarica dal DB e sovrascrivi solo i campi editabili:
        try {
            Utente dbUser = utenteDao.findById(sessionUser.getId());
            if (dbUser != null) {
                toUpdate = dbUser;
            } else {
                // fallback: usa sessionUser come base
                toUpdate = sessionUser;
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Impossibile ricaricare utente prima update: " + e.getMessage(), e);
            toUpdate = sessionUser;
        }

        // aggiorna campi editabili
        if (nome != null) toUpdate.setNome(nome.trim());
        if (cognome != null) toUpdate.setCognome(cognome.trim());
        toUpdate.setEmail(email.trim());
        toUpdate.setTelefono(telefono != null ? telefono.trim() : null);
        toUpdate.setBio(bio != null ? bio.trim() : null);

        // esegui update via DAO
        try {
            boolean ok = utenteDao.update(toUpdate);
            if (ok) {
                // ricarica utente aggiornato e metti in sessione
                try {
                    Utente fresh = utenteDao.findById(toUpdate.getId());
                    if (fresh != null) s.setAttribute("user", fresh);
                    else s.setAttribute("user", toUpdate);
                } catch (SQLException ignore) {
                    s.setAttribute("user", toUpdate);
                }
                s.setAttribute("flashSuccess", "Profilo aggiornato con successo.");
            } else {
                s.setAttribute("flashError", "Errore durante aggiornamento profilo.");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Errore DB update profilo id=" + toUpdate.getId() + ": " + e.getMessage(), e);
            s.setAttribute("flashError", "Errore DB durante aggiornamento profilo.");
        }

        resp.sendRedirect(req.getContextPath() + "/staff/profile");
    }
}
