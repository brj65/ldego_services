package tech.bletchleypark.tools;

import java.util.Date;
import java.util.Random;

public class StringTools {

    private final static String alphabet = "ABCDFGHJKMNPRSTVWXYZ23456789";

    public static final String buildRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    public static boolean isNumeric(String inputString) {
        if (inputString == null || inputString.length() == 0) {
            return false;
        } else {
            for (char c : inputString.toCharArray()) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
        }
        return true;
    }
}
