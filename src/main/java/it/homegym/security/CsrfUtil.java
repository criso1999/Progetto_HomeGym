package it.homegym.security;

import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {
    private static final SecureRandom rnd = new SecureRandom();

    private CsrfUtil() {}

    public static String generateToken() {
        byte[] b = new byte[32];
        rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
