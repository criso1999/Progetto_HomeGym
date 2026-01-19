package it.homegym.controller;

import it.homegym.dao.UtenteDAO;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

@WebServlet("/staff/clients")
public class StaffClientsServlet extends HttpServlet {
    private UtenteDAO dao;

    @Override
    public void init() throws ServletException {
        try {
            dao = new UtenteDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare UtenteDAO", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Utente current = (Utente) session.getAttribute("user");

        try {
            List<Utente> clients;
            // se trainer: mostra i suoi clienti (INCLUSI deleted) così può ripristinare
            if ("PERSONALE".equals(current.getRuolo())) {
                clients = dao.listClientsByTrainerIncludingDeleted(current.getId());
            } else if ("PROPRIETARIO".equals(current.getRuolo())) {
                // admin: mostra tutti gli utenti (o tutti i clienti, a seconda della policy)
                // qui uso listAll() (mostra deleted) così l'admin può ripristinare
                clients = dao.listAll();
            } else {
                // utenti non autorizzati non dovrebbero arrivare qui; fallback a lista vuota
                clients = Collections.emptyList();
            }

            // Lista clienti disponibili per assegnare (non deleted e senza trainer)
            List<Utente> availableClients = dao.listAvailableClientsForAssign();

            // Lista trainer per select
            List<Utente> trainers = dao.listByRole("PERSONALE");

            // mappa trainerId -> "Nome Cognome"
            Map<Integer, String> trainerNames = new HashMap<>();
            for (Utente t : trainers) {
                trainerNames.put(t.getId(), t.getNome() + " " + t.getCognome());
            }

            req.setAttribute("clients", clients);
            req.setAttribute("availableClients", availableClients);
            req.setAttribute("trainers", trainers);
            req.setAttribute("trainerNames", trainerNames);
            req.setAttribute("currentUser", current);

            req.getRequestDispatcher("/WEB-INF/views/staff/clients.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Errore caricamento clienti", e);
        }
    }
}
