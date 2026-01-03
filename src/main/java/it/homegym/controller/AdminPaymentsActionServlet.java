package it.homegym.controller;

import it.homegym.dao.PaymentDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/payments/action")
public class AdminPaymentsActionServlet extends HttpServlet {
    private PaymentDAO dao;

    @Override
    public void init() throws ServletException { super.init(); dao = new PaymentDAO(); }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idStr = req.getParameter("id");
        String action = req.getParameter("action");
        if (idStr == null || action == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            if ("markPaid".equals(action)) {
                dao.updateStatus(id, "PAID");
            } else if ("refund".equals(action)) {
                dao.updateStatus(id, "REFUNDED");
            } else if ("cancel".equals(action)) {
                dao.updateStatus(id, "CANCELLED");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
