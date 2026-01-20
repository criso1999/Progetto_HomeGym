package it.homegym.controller;

import it.homegym.dao.PaymentDAO;
import it.homegym.dao.SessionDAO;
import it.homegym.dao.UtenteDAO;
import it.homegym.model.Payment;
import it.homegym.model.TrainingSession;
import it.homegym.model.Utente;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@WebServlet("/client/sessions/new")
public class ClientSessionFormServlet extends HttpServlet {

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
            List<Utente> trainers = utenteDao.listByRole("PERSONALE");
            req.setAttribute("trainers", trainers);
            req.getRequestDispatcher("/WEB-INF/views/client/session-form.jsp").forward(req, resp);
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

            LocalDateTime ldt = LocalDateTime.parse(scheduledAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Date scheduledDate = new Date(Timestamp.valueOf(ldt).getTime());

            Integer duration = null;
            try { duration = Integer.parseInt(durationStr); } catch (Exception ignored) {}

            BigDecimal amount = new BigDecimal(amountStr);

            // crea session booking come TrainingSession
            TrainingSession booking = new TrainingSession();
            booking.setUserId(current.getId());
            booking.setTrainer(trainer.getNome() + " " + trainer.getCognome() + " <" + trainer.getEmail() + ">");
            booking.setWhen(scheduledDate);
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
            p.setStatus("PAID");

            int paymentId = paymentDao.create(p); // assume create returns generated id
            if (paymentId <= 0) {
                s.setAttribute("flashError", "Prenotazione salvata ma pagamento non registrato.");
                resp.sendRedirect(req.getContextPath() + "/client/sessions");
                return;
            }

            // opzionale: se vuoi collegare payment -> session, aggiungi update su session per impostare payment_id
            s.setAttribute("flashSuccess", "Prenotazione creata e pagamento registrato (id pagamento: " + paymentId + ").");
            resp.sendRedirect(req.getContextPath() + "/client/sessions");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
