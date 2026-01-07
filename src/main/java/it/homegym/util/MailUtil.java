package it.homegym.util;
 
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
 
public final class MailUtil {
 
    private MailUtil() {}
 
    public static void send(String to, String subject, String bodyHtml) throws MessagingException {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv("SMTP_PORT");
        String user = System.getenv("SMTP_USER");
        String pass = System.getenv("SMTP_PASSWORD");
        String from = System.getenv("MAIL_FROM");
        String useSsl = System.getenv("SMTP_SSL"); // "true"/"false"
 
        if (host == null || port == null || from == null) {
            throw new IllegalStateException("SMTP config mancante (SMTP_HOST/SMTP_PORT/MAIL_FROM).");
        }
 
        Properties props = new Properties();
        boolean auth = user != null && !user.isBlank();
        props.put("mail.smtp.auth", auth ? "true" : "false");
        props.put("mail.smtp.starttls.enable", "true"); // STARTTLS (Gmail)
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
 
        // Optional: SSL socket factory when SMTP_SSL=true (rare with Gmail STARTTLS)
        if ("true".equalsIgnoreCase(useSsl)) {
            props.put("mail.smtp.ssl.enable", "true");
        }
 
        Session session;
        if (auth) {
            Authenticator authr = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass != null ? pass : "");
                }
            };
            session = Session.getInstance(props, authr);
        } else {
            session = Session.getInstance(props);
        }
 
        // basic retry
        int maxTries = 2;
        MessagingException lastEx = null;
        for (int attempt = 1; attempt <= maxTries; attempt++) {
            try {
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(from));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
                msg.setSubject(subject);
                msg.setSentDate(new java.util.Date());
                msg.setContent(bodyHtml, "text/html; charset=UTF-8");
                Transport.send(msg);
                return;
            } catch (MessagingException mex) {
                lastEx = mex;
                // optional: sleep small backoff
                try { Thread.sleep(500 * attempt); } catch (InterruptedException ignored) {}
            }
        }
        throw lastEx;
    }
}
