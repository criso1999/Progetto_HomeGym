package it.homegym.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class RecaptchaVerifier {
    private RecaptchaVerifier() {}

    /**
     * Verifica il token col servizio Google.
     * @param secret la RECAPTCHA_SECRET (env)
     * @param response il valore g-recaptcha-response dal client
     * @param remoteIp opzionale: ip del client
     * @return true se Google risponde success:true
     * @throws Exception in caso di errori di rete
     */
    public static boolean verify(String secret, String response, String remoteIp) throws Exception {
        if (secret == null || secret.isBlank()) {
            String disabled = System.getenv("RECAPTCHA_DISABLED");
            if ("true".equalsIgnoreCase(disabled)) {
                System.out.println("reCAPTCHA disabled (dev). Skipping verification.");
                return true;
            }
            // in dev puoi scegliere di bypassare o considerare non valido; qui ritorniamo false
            throw new IllegalStateException("RECAPTCHA_SECRET non impostata");
        }
        if (response == null || response.isBlank()) return false;

        StringBuilder post = new StringBuilder();
        post.append("secret=").append(URLEncoder.encode(secret, StandardCharsets.UTF_8));
        post.append("&response=").append(URLEncoder.encode(response, StandardCharsets.UTF_8));
        if (remoteIp != null && !remoteIp.isBlank()) {
            post.append("&remoteip=").append(URLEncoder.encode(remoteIp, StandardCharsets.UTF_8));
        }

        byte[] postData = post.toString().getBytes(StandardCharsets.UTF_8);

        URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData);
        }

        int status = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                status >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String json = sb.toString();
            // semplice check: cerca "success": true
            return json.contains("\"success\": true");
        } finally {
            conn.disconnect();
        }
    }
}
