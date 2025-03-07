package zw.co.afc.orbit.outpost.troop.util;
import java.util.regex.Pattern;

public class AnsiCleaner {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    public static String removeAnsiCodes(String input) {
        if (input == null) {
            return ""; // or you could return null, depending on your use case
        }
        return ANSI_PATTERN.matcher(input).replaceAll("");
    }
}
