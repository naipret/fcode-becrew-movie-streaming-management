package fcode.becrew.moviestreamingmanagement.controller;

import fcode.becrew.moviestreamingmanagement.model.Subscriber;
import fcode.becrew.moviestreamingmanagement.util.CsvUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Controller managing subscriber registration, subscription tier polymorphism,
 * phone number uniqueness invariants, and atomic CSV persistence.
 */
public class SubscriberController {

    private static final String CSV_HEADER = "id,fullName,phone,email,tier,isDeleted";

    private final Path csvPath;
    private final List<Subscriber> subscribers;

    /**
     * Constructs a SubscriberController.
     *
     * @param csvPath the filesystem Path to data/subscribers.csv
     */
    public SubscriberController(Path csvPath) {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path cannot be null.");
        }
        this.csvPath = csvPath;
        this.subscribers = new ArrayList<>();
        loadFromCsv();
    }

    /**
     * Loads subscriber profiles from persistent CSV storage into memory.
     */
    public final void loadFromCsv() {
        this.subscribers.clear();
        List<String[]> rows = CsvUtil.readCsv(this.csvPath);
        if (rows.isEmpty()) {
            return;
        }

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 6) {
                continue;
            }
            try {
                String id = row[0].trim();
                String fullName = row[1].trim();
                String phone = row[2].trim();
                String email = row[3].trim();
                String tier = row[4].trim();
                boolean isDeleted = Boolean.parseBoolean(row[5].trim());

                this.subscribers.add(Subscriber.create(tier, id, fullName, phone, email, isDeleted));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Persists all in-memory subscriber records atomically to the CSV file.
     */
    public void saveToCsv() {
        List<String[]> rows = new ArrayList<>(this.subscribers.size() + 1);
        rows.add(CSV_HEADER.split(","));
        for (int i = 0; i < this.subscribers.size(); i++) {
            Subscriber sub = this.subscribers.get(i);
            rows.add(new String[] {
                    sub.getId(),
                    sub.getFullName(),
                    sub.getPhone(),
                    sub.getEmail(),
                    sub.getTierName(),
                    Boolean.toString(sub.isDeleted())
            });
        }
        CsvUtil.writeToFile(this.csvPath, rows);
    }

    /**
     * Registers a new subscriber after verifying phone number uniqueness.
     *
     * @param fullName the subscriber full name
     * @param phone    the 10-digit mobile phone number
     * @param email    the subscriber email address
     * @param tier     the subscription tier ("Standard" or "Premium")
     * @return the created Subscriber entity
     * @throws IllegalArgumentException if phone already exists or inputs are
     *                                  invalid
     */
    public Subscriber registerSubscriber(String fullName, String phone, String email, String tier) {
        String cleanPhone = InputValidator.normalizePhone(phone);
        Subscriber existing = findByPhone(cleanPhone);
        if (existing != null) {
            throw new IllegalArgumentException("Active subscriber with phone '" + cleanPhone + "' already exists.");
        }

        String nextId = generateNextId();
        Subscriber subscriber = Subscriber.create(tier, nextId, fullName, cleanPhone, email, false);
        this.subscribers.add(subscriber);
        saveToCsv();
        return subscriber;
    }

    /**
     * Updates an existing subscriber's profile and tier.
     *
     * @param id       the subscriber identifier
     * @param fullName the updated full name
     * @param phone    the updated phone number
     * @param email    the updated email address
     * @param tier     the updated subscription tier
     * @throws IllegalArgumentException if subscriber not found or phone conflicts
     */
    public void updateSubscriber(String id, String fullName, String phone, String email, String tier) {
        Subscriber existing = findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Subscriber not found with ID: " + id);
        }

        String cleanPhone = InputValidator.normalizePhone(phone);
        if (!existing.getPhone().equals(cleanPhone)) {
            Subscriber phoneConflict = findByPhone(cleanPhone);
            if (phoneConflict != null && !phoneConflict.getId().equals(existing.getId())) {
                throw new IllegalArgumentException("Active subscriber with phone '" + cleanPhone + "' already exists.");
            }
        }

        Subscriber updated = Subscriber.create(tier, existing.getId(), fullName, cleanPhone, email,
                existing.isDeleted());
        for (int i = 0; i < this.subscribers.size(); i++) {
            if (this.subscribers.get(i).getId().equals(existing.getId())) {
                this.subscribers.set(i, updated);
                break;
            }
        }
        saveToCsv();
    }

    /**
     * Soft-deletes a subscriber profile.
     *
     * @param id the subscriber identifier to soft-delete
     * @throws IllegalArgumentException if subscriber is not found
     */
    public void deleteSubscriber(String id) {
        Subscriber sub = findById(id);
        if (sub == null) {
            throw new IllegalArgumentException("Subscriber not found with ID: " + id);
        }
        sub.setDeleted(true);
        saveToCsv();
    }

    /**
     * Finds an active subscriber by identifier.
     *
     * @param id the identifier string, may be shorthand
     * @return the active Subscriber, or null if nonexistent or soft-deleted
     */
    public Subscriber findById(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'U');
            for (int i = 0; i < this.subscribers.size(); i++) {
                Subscriber sub = this.subscribers.get(i);
                if (sub.getId().equalsIgnoreCase(canonicalId) && !sub.isDeleted()) {
                    return sub;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds a subscriber by identifier regardless of soft-delete status.
     *
     * @param id the identifier string
     * @return the Subscriber entity, or null if nonexistent
     */
    public Subscriber findByIdIncludeDeleted(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'U');
            for (int i = 0; i < this.subscribers.size(); i++) {
                Subscriber sub = this.subscribers.get(i);
                if (sub.getId().equalsIgnoreCase(canonicalId)) {
                    return sub;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds an active subscriber by normalized phone number.
     *
     * @param phone the 10-digit phone number
     * @return matching active Subscriber, or null if none found
     */
    public Subscriber findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        try {
            String cleanPhone = InputValidator.normalizePhone(phone);
            for (int i = 0; i < this.subscribers.size(); i++) {
                Subscriber sub = this.subscribers.get(i);
                if (!sub.isDeleted() && sub.getPhone().equals(cleanPhone)) {
                    return sub;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Searches active subscribers across ID, name, phone, email, or tier name.
     *
     * @param keyword the search query keyword
     * @return list of matching active Subscriber entities
     */
    public List<Subscriber> search(String keyword) {
        List<Subscriber> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveSubscribers();
        }
        String query = keyword.trim().toLowerCase(Locale.US);

        for (int i = 0; i < this.subscribers.size(); i++) {
            Subscriber sub = this.subscribers.get(i);
            if (sub.isDeleted()) {
                continue;
            }
            if (sub.getId().toLowerCase(Locale.US).contains(query)
                    || sub.getFullName().toLowerCase(Locale.US).contains(query)
                    || sub.getPhone().contains(query)
                    || sub.getEmail().toLowerCase(Locale.US).contains(query)
                    || sub.getTierName().toLowerCase(Locale.US).contains(query)) {
                results.add(sub);
            }
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Returns an unmodifiable list of all active subscribers.
     *
     * @return unmodifiable list of active Subscriber entities
     */
    public List<Subscriber> getAllActiveSubscribers() {
        List<Subscriber> active = new ArrayList<>();
        for (int i = 0; i < this.subscribers.size(); i++) {
            Subscriber sub = this.subscribers.get(i);
            if (!sub.isDeleted()) {
                active.add(sub);
            }
        }
        return Collections.unmodifiableList(active);
    }

    private String generateNextId() {
        int max = 0;
        for (int i = 0; i < this.subscribers.size(); i++) {
            String rawId = this.subscribers.get(i).getId();
            if (rawId.length() >= 2 && rawId.charAt(0) == 'U') {
                try {
                    int num = Integer.parseInt(rawId.substring(1));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format(Locale.US, "U%05d", max + 1);
    }
}
