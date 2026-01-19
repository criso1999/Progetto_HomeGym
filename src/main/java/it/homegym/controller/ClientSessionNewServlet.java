package it.homegym.controller;

import it.homegym.dao.PaymentDAO;
import it.homegym.dao.SessionDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Payment;
import it.homegym.model.SessionBooking;
import it.homegym.model.Utente;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/client/sessions/new")
public class ClientSessionNewServlet extends HttpServlet {

    private UtenteDAO utenteDao;
    private SessionDAO sessionDao;
    private PaymentDAO paymentDao;

    @Override
    public void init() throws ServletException {
        try {
            utenteDao = new UtenteDAO();
            sessionDao = new SessionDAO();
            paymentDao = new PaymentDAO();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // mostra form
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            // lista trainer per select
            List<Utente> trainers = utenteDao.listByRole("PERSONALE");
            req.setAttribute("trainers", trainers);
            req.getRequestDispatcher("/WEB-INF/views/client/sessions/new.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // crea session + pagamento
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Utente current = (Utente) s.getAttribute("user");

        String trainerIdStr = req.getParameter("trainerId");
        String scheduledAtStr = req.getParameter("scheduledAt"); // expects yyyy-MM-ddTHH:mm (datetime-local)
        String durationStr = req.getParameter("durationMinutes");
        String notes = req.getParameter("notes");
        String amountStr = req.getParameter("amount");

        if (trainerIdStr == null || trainerIdStr.isBlank() || scheduledAtStr == null || scheduledAtStr.isBlank() || amountStr == null || amountStr.isBlank()) {
            req.setAttribute("error", "Compila tutti i campi obbligatori (trainer, data/ora, importo).");
            doGet(req, resp);
            return;
        }

        try {
            int trainerId = Integer.parseInt(trainerIdStr);
            Utente trainer = utenteDao.findById(trainerId);
            if (trainer == null) {
                req.setAttribute("error", "Trainer non trovato.");
                doGet(req, resp);
                return;
            }

            // parse scheduledAt (datetime-local -> yyyy-MM-dd'T'HH:mm)
            LocalDateTime ldt = LocalDateTime.parse(scheduledAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Timestamp scheduledTs = Timestamp.valueOf(ldt);

            Integer duration = null;
            try { duration = Integer.parseInt(durationStr); } catch (Exception ignored) {}

            // importo
            BigDecimal amount = new BigDecimal(amountStr);

            // crea session booking
            SessionBooking booking = new SessionBooking();
            booking.setUserId(current.getId());
            // salviamo il nome del trainer come stringa (colonna trainer VARCHAR(100))
            booking.setTrainer(trainer.getNome() + " " + trainer.getCognome() + " <" + trainer.getEmail() + ">");
            booking.setScheduledAt(scheduledTs);
            booking.setDurationMinutes(duration);
            booking.setNotes(notes);

            int sessionId = sessionDao.create(booking);
            if (sessionId <= 0) {
                s.setAttribute("flashError", "Impossibile creare la prenotazione.");
                resp.sendRedirect(req.getContextPath() + "/client/sessions");
                return;
            }

            // crea payment (simulato: status PAID)
            Payment p = new Payment();
            p.setUserId(current.getId());
            p.setAmount(amount);
            p.setCurrency("EUR");
            p.setStatus("PAID"); // se vuoi simulare PENDING, cambialo

            int paymentId = paymentDao.create(p);
            if (paymentId <= 0) {
                // opzionale: roll-back della session? qui lo lasciamo, ma segnaliamo errore
                s.setAttribute("flashError", "Prenotazione salvata ma pagamento non registrato.");
                resp.sendRedirect(req.getContextPath() + "/client/sessions");
                return;
            }

            s.setAttribute("flashSuccess", "Prenotazione creata e pagamento registrato (id pagamento: " + paymentId + ").");
            resp.sendRedirect(req.getContextPath() + "/client/sessions");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
