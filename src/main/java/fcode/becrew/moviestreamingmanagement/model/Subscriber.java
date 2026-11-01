package fcode.becrew.moviestreamingmanagement.model;

import fcode.becrew.moviestreamingmanagement.util.InputValidator;

/**
 * Abstract base class representing a registered subscriber on the streaming
 * platform.
 * Encapsulates common profile attributes and defines polymorphic strategy
 * contracts
 * for monthly billing calculations and concurrent stream capacity.
 */
public abstract class Subscriber {

    private final String id;
    private final String fullName;
    private final String phone;
    private final String email;
    private boolean isDeleted;

    /**
     * Protected constructor initializing base subscriber attributes.
     *
     * @param id        the unique subscriber identifier (e.g. U00001)
     * @param fullName  the subscriber full name
     * @param phone     the 10-digit mobile phone number
     * @param email     the subscriber email address
     * @param isDeleted true if soft-deleted, false if active
     */
    protected Subscriber(String id, String fullName, String phone, String email, boolean isDeleted) {
        this.id = InputValidator.normalizeId(id, 'U');
        this.fullName = InputValidator.normalizeFullName(fullName);
        this.phone = InputValidator.normalizePhone(phone);
        this.email = InputValidator.normalizeEmail(email);
        this.isDeleted = isDeleted;
    }

    /**
     * Static Factory Method instantiating the appropriate concrete Subscriber
     * subclass
     * based on the subscription tier discriminator.
     *
     * @param tier      the subscription tier string ("Standard" or "Premium")
     * @param id        the subscriber identifier
     * @param fullName  the subscriber full name
     * @param phone     the mobile phone number
     * @param email     the email address
     * @param isDeleted true if soft-deleted, false if active
     * @return a concrete Subscriber instance
     * @throws IllegalArgumentException if the tier is unknown or invalid
     */
    public static Subscriber create(String tier, String id, String fullName,
            String phone, String email, boolean isDeleted) {
        if (tier == null || tier.trim().isEmpty()) {
            throw new IllegalArgumentException("Subscriber tier cannot be empty.");
        }
        String cleanTier = tier.trim();
        if ("Standard".equalsIgnoreCase(cleanTier)) {
            return new StandardSubscriber(id, fullName, phone, email, isDeleted);
        }
        if ("Premium".equalsIgnoreCase(cleanTier)) {
            return new PremiumSubscriber(id, fullName, phone, email, isDeleted);
        }
        throw new IllegalArgumentException("Unsupported subscriber tier: " + tier
                + ". Must be 'Standard' or 'Premium'.");
    }

    /**
     * Calculates the monthly subscription billing fee for this subscriber tier.
     *
     * @return the monthly fee in USD
     */
    public abstract double calculateMonthlyFee();

    /**
     * Returns the maximum allowable concurrent streams for this subscriber tier.
     *
     * @return the maximum concurrent stream limit
     */
    public abstract int getMaxConcurrentStreams();

    /**
     * Returns the display name of the subscription tier.
     *
     * @return the tier name
     */
    public abstract String getTierName();

    /**
     * Returns the unique subscriber identifier.
     *
     * @return the subscriber identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the subscriber full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return this.fullName;
    }

    /**
     * Returns the mobile phone number.
     *
     * @return the phone number
     */
    public String getPhone() {
        return this.phone;
    }

    /**
     * Returns the email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Returns whether the subscriber profile is soft-deleted.
     *
     * @return true if soft-deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * Updates the soft-delete status.
     *
     * @param isDeleted true to soft-delete, false to activate
     */
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
