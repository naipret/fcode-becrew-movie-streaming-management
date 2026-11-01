package fcode.becrew.moviestreamingmanagement.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centralized stateless validator and normalizer for entity identifiers,
 * titles, names, emails, phone numbers, numeric metrics, and dates.
 */
public final class InputValidator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Pattern PHONE_REGEX = Pattern.compile("^0[35789]\\d{8}$");
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ASCII_LETTER_CHECK = Pattern.compile(".*[a-zA-Z].*");

    private InputValidator() {
        throw new AssertionError("InputValidator cannot be instantiated.");
    }

    /**
     * Normalizes and validates an entity identifier against a 6-character pattern
     * consisting of a 1-character prefix followed by a 5-digit number.
     *
     * @param rawId  the raw identifier input, may be shorthand
     * @param prefix the expected prefix character
     * @return the normalized canonical 6-character identifier
     * @throws IllegalArgumentException if the raw identifier is invalid or does not
     *                                  match prefix
     */
    public static String normalizeId(String rawId, char prefix) {
        if (rawId == null || rawId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty.");
        }
        String clean = rawId.trim().toUpperCase(Locale.US);
        char expectedPrefix = Character.toUpperCase(prefix);

        if (clean.matches("^\\d{1,5}$")) {
            int num = Integer.parseInt(clean);
            if (num <= 0 || num > 99999) {
                throw new IllegalArgumentException("Identifier number must be between 1 and 99999.");
            }
            return String.format(Locale.US, "%c%05d", expectedPrefix, num);
        }

        if (clean.length() >= 2 && clean.charAt(0) == expectedPrefix) {
            String digitsPart = clean.substring(1);
            if (digitsPart.matches("^\\d{1,5}$")) {
                int num = Integer.parseInt(digitsPart);
                if (num <= 0 || num > 99999) {
                    throw new IllegalArgumentException("Identifier number must be between 1 and 99999.");
                }
                return String.format(Locale.US, "%c%05d", expectedPrefix, num);
            }
        }

        throw new IllegalArgumentException("Invalid identifier format. Expected " + expectedPrefix
                + " followed by digits (e.g. " + expectedPrefix + "00001).");
    }

    /**
     * Validates and cleans non-empty 7-bit ASCII text.
     *
     * @param rawText   the raw text string
     * @param fieldName the display name of the field for error messages
     * @return the trimmed, sanitized string
     * @throws IllegalArgumentException if the text is empty or contains non-ASCII
     *                                  characters
     */
    public static String normalizeText(String rawText, String fieldName) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        String clean = rawText.trim();
        for (int i = 0; i < clean.length(); i++) {
            if (clean.charAt(i) > 127) {
                throw new IllegalArgumentException(fieldName + " must contain 7-bit ASCII characters only.");
            }
        }
        return clean;
    }

    /**
     * Normalizes a subscriber or person full name into standard Title Case.
     *
     * @param rawName the raw name input
     * @return the title-cased normalized full name
     * @throws IllegalArgumentException if the name is empty, has non-ASCII
     *                                  characters, or contains digits
     */
    public static String normalizeFullName(String rawName) {
        String clean = normalizeText(rawName, "Full name");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isDigit(c)) {
                throw new IllegalArgumentException("Full name cannot contain numeric digits.");
            }
        }
        if (!ASCII_LETTER_CHECK.matcher(clean).matches()) {
            throw new IllegalArgumentException("Full name must contain alphabetic characters.");
        }

        String[] parts = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase(Locale.US));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Normalizes and validates a Vietnamese 10-digit mobile phone number.
     *
     * @param rawPhone the raw phone number string
     * @return the normalized 10-digit phone number
     * @throws IllegalArgumentException if phone format is invalid
     */
    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        String clean = rawPhone.replaceAll("[^0-9]", "");
        if (!PHONE_REGEX.matcher(clean).matches()) {
            throw new IllegalArgumentException(
                    "Phone number must be a valid 10-digit mobile number (e.g. 0901234567).");
        }
        return clean;
    }

    /**
     * Normalizes and validates an email address according to RFC 5322 format.
     *
     * @param rawEmail the raw email string
     * @return the trimmed, lowercased email address
     * @throws IllegalArgumentException if email format is invalid
     */
    public static String normalizeEmail(String rawEmail) {
        String clean = normalizeText(rawEmail, "Email address").toLowerCase(Locale.US);
        if (!EMAIL_REGEX.matcher(clean).matches()) {
            throw new IllegalArgumentException("Invalid email format (e.g. user@domain.com).");
        }
        return clean;
    }

    /**
     * Validates that a movie runtime duration is strictly positive.
     *
     * @param durationMinutes the duration in minutes
     * @throws IllegalArgumentException if duration is non-positive
     */
    public static void validatePositiveDuration(int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Movie duration must be greater than zero minutes.");
        }
    }

    /**
     * Validates that a movie rating falls within the 0.0 to 10.0 range.
     *
     * @param rating the rating value
     * @throws IllegalArgumentException if rating is out of range
     */
    public static void validateRating(double rating) {
        if (rating < 0.0 || rating > 10.0) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 10.0 inclusive.");
        }
    }

    /**
     * Validates that a movie release year is between 1888 and current year plus
     * five.
     *
     * @param year the release year to validate
     * @throws IllegalArgumentException if release year is invalid
     */
    public static void validateReleaseYear(int year) {
        int maxYear = LocalDate.now().getYear() + 5;
        if (year < 1888 || year > maxYear) {
            throw new IllegalArgumentException("Release year must be between 1888 and " + maxYear + ".");
        }
    }

    /**
     * Validates that watched minutes is positive and does not exceed maximum
     * allowable threshold.
     *
     * @param watchedMinutes the minutes watched in a session
     * @param maxAllowed     the maximum allowed minutes for the session
     * @throws IllegalArgumentException if watched minutes is non-positive or
     *                                  exceeds maximum allowed
     */
    public static void validateWatchedMinutes(int watchedMinutes, int maxAllowed) {
        if (watchedMinutes <= 0) {
            throw new IllegalArgumentException("Watched minutes must be greater than zero.");
        }
        if (watchedMinutes > maxAllowed) {
            throw new IllegalArgumentException("Watched minutes cannot exceed " + maxAllowed + " minutes.");
        }
    }

    /**
     * Parses a YearMonth string in MM/yyyy format.
     *
     * @param raw the raw input month string
     * @return the parsed YearMonth object
     * @throws IllegalArgumentException if parsing fails
     */
    public static YearMonth parseYearMonth(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Month string cannot be empty.");
        }
        String clean = raw.trim();
        if (clean.matches("^\\d{1,2}$")) {
            int month = Integer.parseInt(clean);
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be between 1 and 12.");
            }
            return YearMonth.of(LocalDate.now().getYear(), month);
        }
        if (!clean.matches("^\\d{1,2}/\\d{4}$")) {
            throw new IllegalArgumentException("Month must follow format MM/yyyy (e.g. 10/2026).");
        }
        String[] parts = clean.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        return YearMonth.of(year, month);
    }

    /**
     * Formats a LocalDateTime into standard display pattern dd/MM/yyyy HH:mm.
     *
     * @param dateTime the LocalDateTime to format
     * @return the formatted timestamp string, or empty if dateTime is null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime == null) ? "" : dateTime.format(TIMESTAMP_FORMATTER);
    }
}
