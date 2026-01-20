package it.homegym.controller;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.homegym.dao.PresenceDAO;
import it.homegym.model.Utente;

@WebServlet("/staff/sessions/qr")
public class QRGenerateServlet extends HttpServlet {
    private PresenceDAO presenceDao;

    @Override
    public void init() {
        presenceDao = new PresenceDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        Utente me = (Utente) session.getAttribute("user");
        if (!"PERSONALE".equals(me.getRuolo()) && !"PROPRIETARIO".equals(me.getRuolo())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }

        String sid = req.getParameter("sessionId");
        int expiresMins = 30; // default
        try { expiresMins = Integer.parseInt(req.getParameter("expires")); } catch (Exception ignored) {}

        Integer sessionId = sid != null && !sid.isBlank() ? Integer.parseInt(sid) : null;
        String token;
        try {
            token = presenceDao.createToken(sessionId, expiresMins);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        // costruisci URL che verrà codificato nel QR
        String base = req.getScheme() + "://" + req.getServerName() + (req.getServerPort()==80||req.getServerPort()==443? "" : ":"+req.getServerPort()) + req.getContextPath();
        String qrUrl = base + "/attendance/check?token=" + token;

        resp.setContentType("image/png");
        // genera QR con ZXing
        try {
            int size = 300;
            com.google.zxing.common.BitMatrix matrix = new com.google.zxing.MultiFormatWriter()
                    .encode(qrUrl, com.google.zxing.BarcodeFormat.QR_CODE, size, size);
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (com.google.zxing.WriterException e) {
            throw new ServletException(e);
        }
    }
}
