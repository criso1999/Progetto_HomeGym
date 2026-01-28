package it.homegym.controller;

import it.homegym.dao.SubscriptionDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/webhook/payment")
public class PaymentWebhookServlet extends HttpServlet {

    private SubscriptionDAO dao;

    @Override
    public void init() {
        dao = new SubscriptionDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Ricevi evento dal provider, verifica signature, deserializza JSON e prendi riferimento subId (dal metadata)
        // Esempio pseudo:
        /*
         { "type": "checkout.session.completed", "data": { "object": { "client_reference_id": "subscription:123", "id":"cs_XXX" } } }
        */
        try (BufferedReader br = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String payload = sb.toString();

            // TODO: verifica signature header (es. Stripe-Signature)
            // Parse payload -> recupera subscription id creato come metadata o client_reference_id
            // Esempio: client_reference_id = "subscription:123"
            // Se pago OK -> dao.activateSubscription(subId, startDate, endDate, providerSubscriptionId)

            // ATTENZIONE: qui implementa la logica di parsing per il provider scelto.
            resp.setStatus(200);
        } catch (Exception e) {
            resp.setStatus(500);
        }
    }
}
