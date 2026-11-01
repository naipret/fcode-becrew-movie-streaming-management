package fcode.becrew.moviestreamingmanagement.model;

/**
 * Concrete subscriber tier providing standard definition and 2 concurrent streams.
 */
public class StandardSubscriber extends Subscriber {

    /**
     * Monthly subscription fee in USD for Standard tier.
     */
    public static final double STANDARD_FEE = 7.99;

    /**
     * Maximum concurrent streams allowed for Standard tier.
     */
    public static final int STANDARD_STREAMS = 2;

    /**
     * Constructs a StandardSubscriber.
     *
     * @param id        the unique subscriber identifier
     * @param fullName  the subscriber full name
     * @param phone     the mobile phone number
     * @param email     the email address
     * @param isDeleted true if soft-deleted, false if active
     */
    public StandardSubscriber(String id, String fullName, String phone, String email, boolean isDeleted) {
        super(id, fullName, phone, email, isDeleted);
    }

    @Override
    public double calculateMonthlyFee() {
        return STANDARD_FEE;
    }

    @Override
    public int getMaxConcurrentStreams() {
        return STANDARD_STREAMS;
    }

    @Override
    public String getTierName() {
        return "Standard";
    }
}
