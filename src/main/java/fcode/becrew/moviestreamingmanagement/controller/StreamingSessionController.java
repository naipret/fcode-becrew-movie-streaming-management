package fcode.becrew.moviestreamingmanagement.controller;

import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.model.StreamingSession;
import fcode.becrew.moviestreamingmanagement.model.Subscriber;
import fcode.becrew.moviestreamingmanagement.util.CsvUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Controller managing streaming playback sessions, synchronization of cumulative
 * movie view counts, completion flags, and atomic CSV storage.
 */
public class StreamingSessionController {

    private static final String CSV_HEADER = "sessionId,subscriberId,movieId,sessionDate,watchedMinutes,isCompleted";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Path csvPath;
    private final List<StreamingSession> sessions;
    private final SubscriberController subscriberController;
    private final MovieController movieController;

    /**
     * Constructs a StreamingSessionController.
     *
     * @param csvPath              the filesystem Path to data/sessions.csv
     * @param subscriberController collaborator for validating subscribers
     * @param movieController      collaborator for validating movies and updating views
     */
    public StreamingSessionController(Path csvPath, SubscriberController subscriberController,
                                      MovieController movieController) {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path cannot be null.");
        }
        if (subscriberController == null) {
            throw new IllegalArgumentException("SubscriberController cannot be null.");
        }
        if (movieController == null) {
            throw new IllegalArgumentException("MovieController cannot be null.");
        }
        this.csvPath = csvPath;
        this.subscriberController = subscriberController;
        this.movieController = movieController;
        this.sessions = new ArrayList<>();
        loadFromCsv();
    }

    /**
     * Loads playback session logs from persistent CSV storage into memory.
     */
    public final void loadFromCsv() {
        this.sessions.clear();
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
                String sessionId = row[0].trim();
                String subscriberId = row[1].trim();
                String movieId = row[2].trim();
                LocalDateTime sessionDate = LocalDateTime.parse(row[3].trim(), ISO_FORMATTER);
                int watchedMinutes = Integer.parseInt(row[4].trim());
                boolean isCompleted = Boolean.parseBoolean(row[5].trim());

                this.sessions.add(new StreamingSession(sessionId, subscriberId, movieId,
                        sessionDate, watchedMinutes, isCompleted));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Persists all in-memory playback sessions atomically to the CSV file.
     */
    public void saveToCsv() {
        List<String[]> rows = new ArrayList<>(this.sessions.size() + 1);
        rows.add(CSV_HEADER.split(","));
        for (int i = 0; i < this.sessions.size(); i++) {
            StreamingSession s = this.sessions.get(i);
            rows.add(new String[]{
                    s.getSessionId(),
                    s.getSubscriberId(),
                    s.getMovieId(),
                    s.getSessionDate().format(ISO_FORMATTER),
                    Integer.toString(s.getWatchedMinutes()),
                    Boolean.toString(s.isCompleted())
            });
        }
        CsvUtil.writeToFile(this.csvPath, rows);
    }

    /**
     * Records a new streaming playback session, validates entities, and syncs view count.
     *
     * @param subscriberId   the subscriber identifier
     * @param movieId        the movie identifier
     * @param watchedMinutes positive integer minutes watched
     * @param sessionDate    the session timestamp
     * @return the logged StreamingSession entity
     * @throws IllegalArgumentException if subscriber or movie is invalid or missing
     */
    public StreamingSession recordSession(String subscriberId, String movieId,
                                          int watchedMinutes, LocalDateTime sessionDate) {
        Subscriber subscriber = this.subscriberController.findById(subscriberId);
        if (subscriber == null) {
            throw new IllegalArgumentException("Active subscriber not found with ID: " + subscriberId);
        }

        Movie movie = this.movieController.findById(movieId);
        if (movie == null) {
            throw new IllegalArgumentException("Active movie not found with ID: " + movieId);
        }

        int maxAllowed = movie.getDurationMinutes() + 30;
        InputValidator.validateWatchedMinutes(watchedMinutes, maxAllowed);

        boolean isCompleted = watchedMinutes >= movie.getDurationMinutes();
        String nextId = generateNextId();
        LocalDateTime validDate = (sessionDate != null) ? sessionDate : LocalDateTime.now();

        StreamingSession session = new StreamingSession(nextId, subscriber.getId(), movie.getId(),
                validDate, watchedMinutes, isCompleted);
        this.sessions.add(session);
        saveToCsv();

        movie.incrementViewCount();
        this.movieController.saveToCsv();

        return session;
    }

    /**
     * Cancels an erroneous playback session and decrements the movie's view count.
     *
     * @param sessionId the session identifier to cancel
     * @throws IllegalArgumentException if session is not found
     */
    public void cancelSession(String sessionId) {
        StreamingSession target = findById(sessionId);
        if (target == null) {
            throw new IllegalArgumentException("Streaming session not found with ID: " + sessionId);
        }

        Movie movie = this.movieController.findByIdIncludeDeleted(target.getMovieId());
        if (movie != null) {
            movie.decrementViewCount();
            this.movieController.saveToCsv();
        }

        this.sessions.remove(target);
        saveToCsv();
    }

    /**
     * Finds a streaming session by identifier.
     *
     * @param sessionId the session identifier
     * @return the StreamingSession entity, or null if not found
     */
    public StreamingSession findById(String sessionId) {
        try {
            String canonicalId = InputValidator.normalizeId(sessionId, 'S');
            for (int i = 0; i < this.sessions.size(); i++) {
                StreamingSession s = this.sessions.get(i);
                if (s.getSessionId().equalsIgnoreCase(canonicalId)) {
                    return s;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Returns an unmodifiable list of all streaming sessions.
     *
     * @return unmodifiable list of all StreamingSession entities
     */
    public List<StreamingSession> getAllSessions() {
        return Collections.unmodifiableList(new ArrayList<>(this.sessions));
    }

    /**
     * Retrieves all streaming sessions associated with a specific subscriber.
     *
     * @param subscriberId the subscriber identifier
     * @return list of matching streaming sessions
     */
    public List<StreamingSession> getSessionsBySubscriber(String subscriberId) {
        List<StreamingSession> results = new ArrayList<>();
        try {
            String canonicalId = InputValidator.normalizeId(subscriberId, 'U');
            for (int i = 0; i < this.sessions.size(); i++) {
                StreamingSession s = this.sessions.get(i);
                if (s.getSubscriberId().equalsIgnoreCase(canonicalId)) {
                    results.add(s);
                }
            }
        } catch (IllegalArgumentException e) {
            return results;
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Retrieves all streaming sessions associated with a specific movie.
     *
     * @param movieId the movie identifier
     * @return list of matching streaming sessions
     */
    public List<StreamingSession> getSessionsByMovie(String movieId) {
        List<StreamingSession> results = new ArrayList<>();
        try {
            String canonicalId = InputValidator.normalizeId(movieId, 'M');
            for (int i = 0; i < this.sessions.size(); i++) {
                StreamingSession s = this.sessions.get(i);
                if (s.getMovieId().equalsIgnoreCase(canonicalId)) {
                    results.add(s);
                }
            }
        } catch (IllegalArgumentException e) {
            return results;
        }
        return Collections.unmodifiableList(results);
    }

    private String generateNextId() {
        int max = 0;
        for (int i = 0; i < this.sessions.size(); i++) {
            String rawId = this.sessions.get(i).getSessionId();
            if (rawId.length() >= 2 && rawId.charAt(0) == 'S') {
                try {
                    int num = Integer.parseInt(rawId.substring(1));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format(Locale.US, "S%05d", max + 1);
    }
}
