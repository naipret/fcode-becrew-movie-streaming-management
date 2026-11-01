package fcode.becrew.moviestreamingmanagement.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility for reading and atomically writing RFC 4180 CSV files.
 * Uses line-oriented buffered I/O, supports quoted fields containing commas
 * or escaped double quotes, strips optional UTF-8 BOM markers, and writes
 * via a temporary sibling file followed by an atomic file move.
 */
public final class CsvUtil {

    private static final char UTF8_BOM = '\uFEFF';

    private CsvUtil() {
        throw new AssertionError("CsvUtil cannot be instantiated.");
    }

    /**
     * Reads all non-empty rows from a CSV file at the specified Path.
     * Returns an empty list if the file does not exist.
     *
     * @param path the Path of the CSV file to read
     * @return a list of string arrays representing parsed CSV rows
     */
    public static List<String[]> readCsv(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("CSV path cannot be null.");
        }
        List<String[]> records = new ArrayList<>();
        if (!Files.exists(path)) {
            return records;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine && !line.isEmpty() && line.charAt(0) == UTF8_BOM) {
                    line = line.substring(1);
                }
                firstLine = false;
                if (line.trim().isEmpty()) {
                    continue;
                }
                records.add(parseLine(line));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file: " + path.getFileName(), e);
        }
        return records;
    }

    /**
     * Atomically writes CSV rows to the specified destination Path via a temporary file.
     *
     * @param targetPath the target file Path
     * @param rows       the list of string arrays representing CSV rows
     */
    public static void writeToFile(Path targetPath, List<String[]> rows) {
        if (targetPath == null) {
            throw new IllegalArgumentException("Target CSV path cannot be null.");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Rows cannot be null.");
        }

        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory: " + parent, e);
            }
        }

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(
                tempPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (int i = 0; i < rows.size(); i++) {
                writer.write(formatLine(rows.get(i)));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // Ignore cleanup failure
            }
            throw new IllegalStateException("Failed to write temporary CSV: " + tempPath.getFileName(), e);
        }

        try {
            Files.move(tempPath, targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackError) {
                throw new IllegalStateException("Failed to atomically replace CSV file: "
                        + targetPath.getFileName(), fallbackError);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to atomically replace CSV file: "
                    + targetPath.getFileName(), e);
        }
    }

    /**
     * Parses a single CSV line conforming to RFC 4180 rules.
     *
     * @param line the raw CSV line string
     * @return an array of parsed string fields
     */
    public static String[] parseLine(String line) {
        if (line == null) {
            return new String[0];
        }
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int length = line.length();

        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * Formats an array of fields into a single RFC 4180 compliant CSV line.
     *
     * @param fields the field values to format
     * @return the escaped, comma-delimited CSV line
     */
    public static String formatLine(String[] fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeField(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV field value according to RFC 4180.
     *
     * @param value the raw field value
     * @return the escaped field value
     */
    public static String escapeField(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")
                || value.startsWith(" ") || value.endsWith(" ");
        if (!mustQuote) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                sb.append("\"\"");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
