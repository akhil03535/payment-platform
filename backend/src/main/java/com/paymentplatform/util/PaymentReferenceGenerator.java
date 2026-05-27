package com.paymentplatform.util;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class PaymentReferenceGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generate() {
        String datePart = LocalDateTime.now().format(FORMATTER);
        StringBuilder randomPart = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            randomPart.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return "PAY-" + datePart + "-" + randomPart;
    }

    public static String generateTransactionRef() {
        String datePart = LocalDateTime.now().format(FORMATTER);
        StringBuilder randomPart = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            randomPart.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return "TXN-" + datePart + "-" + randomPart;
    }

    private PaymentReferenceGenerator() {}
}
