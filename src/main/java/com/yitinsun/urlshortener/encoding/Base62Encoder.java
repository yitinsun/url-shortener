package com.yitinsun.urlshortener.encoding;

public final class Base62Encoder {

    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {
    }

    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (id == 0) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        long value = id;
        while (value > 0) {
            int remainder = (int) (value % BASE);
            builder.append(ALPHABET.charAt(remainder));
            value = value / BASE;
        }
        return builder.reverse().toString();
    }

    public static long decode(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            throw new IllegalArgumentException("shortCode must not be blank");
        }
        long total = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            int index = ALPHABET.indexOf(shortCode.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + shortCode.charAt(i));
            }
            total = Math.multiplyExact(total, BASE);
            total = Math.addExact(total, index);
        }
        return total;
    }
}
