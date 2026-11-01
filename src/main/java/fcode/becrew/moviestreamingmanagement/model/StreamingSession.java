package fcode.becrew.moviestreamingmanagement.model;

import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import java.time.LocalDateTime;

/**
 * Domain record representing an individual playback streaming session by a subscriber.
 */
public class StreamingSession {

    private final String sessionId;
    private final String subscriberId;
    private final String movieId;
    private final LocalDateTime sessionDate;
    private final int watchedMinutes;
    private final boolean isCompleted;

    /**
     * Constructs a StreamingSession record.
     *
     * @param sessionId      the unique session identifier (e.g. S00001)
     * @param subscriberId   the referenced subscriber identifier (e.g. U00001)
     * @param movieId        the referenced movie identifier (e.g. M00001)
     * @param sessionDate    the timestamp when streaming occurred
     * @param watchedMinutes total minutes watched during this session
     * @param isCompleted    true if full runtime was watched, false if partial
     */
    public StreamingSession(String sessionId, String subscriberId, String movieId,
                            LocalDateTime sessionDate, int watchedMinutes, boolean isCompleted) {
        this.sessionId = InputValidator.normalizeId(sessionId, 'S');
        this.subscriberId = InputValidator.normalizeId(subscriberId, 'U');
        this.movieId = InputValidator.normalizeId(movieId, 'M');
        if (sessionDate == null) {
            throw new IllegalArgumentException("Session date cannot be null.");
        }
        if (sessionDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Session date cannot be in the future.");
        }
        this.sessionDate = sessionDate;
        if (watchedMinutes <= 0) {
            throw new IllegalArgumentException("Watched minutes must be greater than zero.");
        }
        this.watchedMinutes = watchedMinutes;
        this.isCompleted = isCompleted;
    }

    /**
     * Returns the unique session identifier.
     *
     * @return the session identifier
     */
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Returns the referenced subscriber identifier.
     *
     * @return the subscriber identifier
     */
    public String getSubscriberId() {
        return this.subscriberId;
    }

    /**
     * Returns the referenced movie identifier.
     *
     * @return the movie identifier
     */
    public String getMovieId() {
        return this.movieId;
    }

    /**
     * Returns the session timestamp.
     *
     * @return the session date and time
     */
    public LocalDateTime getSessionDate() {
        return this.sessionDate;
    }

    /**
     * Returns total minutes watched in this session.
     *
     * @return watched minutes
     */
    public int getWatchedMinutes() {
        return this.watchedMinutes;
    }

    /**
     * Returns whether the movie was watched to completion.
     *
     * @return true if complete, false otherwise
     */
    public boolean isCompleted() {
        return this.isCompleted;
    }
}
