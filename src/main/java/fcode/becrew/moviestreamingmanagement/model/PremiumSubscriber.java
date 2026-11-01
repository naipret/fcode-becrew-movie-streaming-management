package fcode.becrew.moviestreamingmanagement.model;

/**
 * Concrete subscriber tier providing ultra high-definition and 4 concurrent
 * streams.
 */
public class PremiumSubscriber extends Subscriber {

    /**
     * Monthly subscription fee in USD for Premium tier.
     */
    public static final double PREMIUM_FEE = 14.99;

    /**
     * Maximum concurrent streams allowed for Premium tier.
     */
    public static final int PREMIUM_STREAMS = 4;

    /**
     * Constructs a PremiumSubscriber.
     *
     * @param id        the unique subscriber identifier
     * @param fullName  the subscriber full name
     * @param phone     the mobile phone number
     * @param email     the email address
     * @param isDeleted true if soft-deleted, false if active
     */
    public PremiumSubscriber(String id, String fullName, String phone, String email, boolean isDeleted) {
        super(id, fullName, phone, email, isDeleted);
    }

    @Override
    public double calculateMonthlyFee() {
        return PREMIUM_FEE;
    }

    @Override
    public int getMaxConcurrentStreams() {
        return PREMIUM_STREAMS;
    }

    @Override
    public String getTierName() {
        return "Premium";
    }
}
