package fcode.becrew.moviestreamingmanagement.util;

import java.io.IOException;
import java.time.YearMonth;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Utility for interactive console input reading, range-checked numeric prompts,
 * date parsing, confirmation dialogs, and terminal screen clearing.
 */
public final class ConsoleUtil {

    private ConsoleUtil() {
        throw new AssertionError("ConsoleUtil cannot be instantiated.");
    }

    /**
     * Clears the terminal screen.
     */
    public static void clearScreen() {
        System.out.println();
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")
                    && System.console() != null) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.print("");
        }
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    /**
     * Reads a line of text, returning null if the operator enters 0 to cancel.
     *
     * @param prompt the prompt message to display
     * @return non-empty string, or null if cancelled with 0
     */
    public static String readString(String prompt) {
        while (true) {
            String input = readOptionalString(prompt);
            if ("0".equals(input)) {
                return null;
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[ERROR] Input cannot be blank. Enter a value or 0 to cancel.");
        }
    }

    /**
     * Reads a line of text, allowing empty input.
     *
     * @param prompt the prompt message to display
     * @return trimmed input string, may be empty
     */
    public static String readOptionalString(String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print(prompt);
        }
        Scanner scanner = ScannerManager.getInstance().getScanner();
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("End of input stream reached.");
        }
        return scanner.nextLine().trim();
    }

    /**
     * Reads an integer within a specified closed range [min, max].
     *
     * @param prompt the prompt message to display
     * @param min    minimum allowable value
     * @param max    maximum allowable value
     * @return validated integer value
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String input = readOptionalString(prompt);
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("[ERROR] Please enter an integer between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format. Please enter an integer.");
            }
        }
    }

    /**
     * Reads an integer with an optional default value on pressing Enter, or null on
     * 0.
     *
     * @param prompt       the prompt message
     * @param min          minimum allowable value
     * @param max          maximum allowable value
     * @param defaultValue the default returned on pressing Enter
     * @return integer value, or defaultValue on empty, or null on 0
     */
    public static Integer readOptionalInt(String prompt, int min, int max, Integer defaultValue) {
        while (true) {
            String input = readOptionalString(prompt);
            if (input.isEmpty()) {
                return defaultValue;
            }
            if ("0".equals(input)) {
                return null;
            }
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("[ERROR] Please enter an integer between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format. Please enter an integer.");
            }
        }
    }

    public static Double readOptionalDouble(String prompt, double min, double max, Double defaultValue) {
        while (true) {
            String input = readOptionalString(prompt);
            if (input.isEmpty()) {
                return defaultValue;
            }
            if ("0".equals(input)) {
                return null;
            }
            try {
                double val = Double.parseDouble(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("[ERROR] Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format. Please enter a valid decimal number.");
            }
        }
    }

    /**
     * Reads a YearMonth in MM/yyyy format with optional default to current month.
     *
     * @param prompt              the prompt message
     * @param allowDefaultCurrent true to return YearMonth.now() on pressing Enter
     * @return parsed YearMonth, or null if cancelled with 0
     */
    public static YearMonth readYearMonth(String prompt, boolean allowDefaultCurrent) {
        while (true) {
            String input = readOptionalString(prompt);
            if ("0".equals(input)) {
                return null;
            }
            if (input.isEmpty() && allowDefaultCurrent) {
                return YearMonth.now();
            }
            try {
                return InputValidator.parseYearMonth(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    /**
     * Prompts the operator for a binary confirmation [Y/N].
     *
     * @param prompt the confirmation question prompt
     * @return true if the operator answers Y or Yes; false if N, No, or 0
     */
    public static boolean confirm(String prompt) {
        while (true) {
            String input = readOptionalString(prompt + " [Y/N]: ").toLowerCase();
            if ("y".equals(input) || "yes".equals(input)) {
                return true;
            }
            if ("n".equals(input) || "no".equals(input) || "0".equals(input)) {
                return false;
            }
            System.out.println("[ERROR] Please enter 'Y' for yes or 'N' for no (or 0 to cancel).");
        }
    }
}
