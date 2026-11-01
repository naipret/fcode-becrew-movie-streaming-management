package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.controller.MovieController;
import fcode.becrew.moviestreamingmanagement.controller.StreamingSessionController;
import fcode.becrew.moviestreamingmanagement.controller.SubscriberController;
import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.model.StreamingSession;
import fcode.becrew.moviestreamingmanagement.model.Subscriber;
import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import fcode.becrew.moviestreamingmanagement.util.TableFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interactive 80-column console menu for managing streaming playback sessions:
 * recording watch logs, viewing session history with filtering, and cancelling
 * sessions.
 */
public class StreamingSessionMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Record Streaming Session",
            "View All Streaming Sessions",
            "Cancel Streaming Session"
    };

    private static final String TABLE_BORDER = "+--------+--------+--------+------------------+----------+----------+----------+";
    private static final String TABLE_HEADER = "| SessID | SubID  | MovID  | Session Date     | Watched  | Duration | Status   |";

    private final StreamingSessionController sessionController;
    private final SubscriberController subscriberController;
    private final MovieController movieController;

    /**
     * Constructs a StreamingSessionMenu.
     *
     * @param sessionController    collaborator for session logging
     * @param subscriberController collaborator for subscriber verification
     * @param movieController      collaborator for movie verification
     */
    public StreamingSessionMenu(StreamingSessionController sessionController,
            SubscriberController subscriberController,
            MovieController movieController) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM > [4] SESSIONS", MENU_OPTIONS);
        if (sessionController == null || subscriberController == null || movieController == null) {
            throw new IllegalArgumentException("Controllers cannot be null.");
        }
        this.sessionController = sessionController;
        this.subscriberController = subscriberController;
        this.movieController = movieController;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                handleRecordSession();
                break;
            case 2:
                handleViewAllSessions();
                break;
            case 3:
                handleCancelSession();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    private void handleRecordSession() {
        while (true) {
            String subId = ConsoleUtil.readString("Enter Subscriber ID (or 0 to return): ");
            if (subId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Subscriber subscriber = this.subscriberController.findById(subId);
            if (subscriber == null) {
                System.out.println("[ERROR] Active subscriber not found with ID: " + subId);
                printDivider();
                continue;
            }

            String movId = ConsoleUtil.readString("Enter Movie ID (or 0 to cancel): ");
            if (movId == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            Movie movie = this.movieController.findById(movId);
            if (movie == null) {
                System.out.println("[ERROR] Active movie not found with ID: " + movId);
                printDivider();
                continue;
            }

            System.out.println("  Movie: " + movie.getTitle() + " | Duration: " + movie.getDurationMinutes() + " min");
            int maxAllowed = movie.getDurationMinutes() + 30;
            Integer watchedMinutes = ConsoleUtil.readOptionalInt(
                    "Enter minutes watched (1-" + maxAllowed + ") [Enter = " + movie.getDurationMinutes() + "]: ",
                    1, maxAllowed, movie.getDurationMinutes());
            if (watchedMinutes == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            boolean isCompleted = watchedMinutes >= movie.getDurationMinutes();

            System.out.println();
            System.out.println(TABLE_BORDER);
            System.out.println(TABLE_HEADER);
            System.out.println(TABLE_BORDER);
            printSessionRow("PREVIEW", subscriber.getId(), movie.getId(), now,
                    watchedMinutes, movie.getDurationMinutes(), isCompleted);
            System.out.println(TABLE_BORDER);

            if (!ConsoleUtil.confirm("Confirm recording this streaming session?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                StreamingSession session = this.sessionController.recordSession(
                        subscriber.getId(), movie.getId(), watchedMinutes, now);
                System.out.println("[OK] Streaming session " + session.getSessionId() + " logged successfully.");
                if (session.isCompleted()) {
                    System.out.println("  [INFO] Movie runtime completed. Status flagged as Complete.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleViewAllSessions() {
        while (true) {
            List<StreamingSession> all = this.sessionController.getAllSessions();
            renderSessionTable(all);

            System.out.println("Options: [Filter by Subscriber ID or Movie ID] | [Keyword] | [0] Return");
            String input = ConsoleUtil.readOptionalString("Enter filter: ");
            if ("0".equals(input)) {
                return;
            }
            if (input.isEmpty()) {
                continue;
            }

            List<StreamingSession> subMatches = this.sessionController.getSessionsBySubscriber(input);
            if (!subMatches.isEmpty()) {
                System.out.println();
                System.out.println("  Sessions for Subscriber: " + input);
                renderSessionTable(subMatches);
                continue;
            }

            List<StreamingSession> movMatches = this.sessionController.getSessionsByMovie(input);
            if (!movMatches.isEmpty()) {
                System.out.println();
                System.out.println("  Sessions for Movie: " + input);
                renderSessionTable(movMatches);
                continue;
            }

            List<StreamingSession> keywordMatches = new ArrayList<>();
            String query = input.toLowerCase(Locale.US);
            for (int i = 0; i < all.size(); i++) {
                StreamingSession s = all.get(i);
                if (s.getSessionId().toLowerCase(Locale.US).contains(query)
                        || s.getSubscriberId().toLowerCase(Locale.US).contains(query)
                        || s.getMovieId().toLowerCase(Locale.US).contains(query)
                        || InputValidator.formatDateTime(s.getSessionDate()).contains(query)) {
                    keywordMatches.add(s);
                }
            }

            if (keywordMatches.isEmpty()) {
                System.out.println("[INFO] No streaming sessions found matching '" + input + "'.");
            } else {
                System.out.println();
                System.out.println("  Search Results for: '" + input + "'");
                renderSessionTable(keywordMatches);
            }
        }
    }

    private void handleCancelSession() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Session ID to cancel (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            StreamingSession session = this.sessionController.findById(rawId);
            if (session == null) {
                System.out.println("[ERROR] Streaming session not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleSessionTable(session);

            if (!ConsoleUtil
                    .confirm("Confirm cancelling session " + session.getSessionId() + " (will revert view count)?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.sessionController.cancelSession(session.getSessionId());
                System.out.println("[OK] Streaming session " + session.getSessionId() + " cancelled successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void renderSessionTable(List<StreamingSession> sessionList) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        if (sessionList.isEmpty()) {
            System.out.println("|"
                    + TableFormatter.center("No streaming sessions logged.", TableFormatter.SCREEN_WIDTH - 2) + "|");
        } else {
            for (int i = 0; i < sessionList.size(); i++) {
                StreamingSession s = sessionList.get(i);
                Movie m = this.movieController.findByIdIncludeDeleted(s.getMovieId());
                int duration = (m == null) ? s.getWatchedMinutes() : m.getDurationMinutes();
                printSessionRow(s.getSessionId(), s.getSubscriberId(), s.getMovieId(),
                        s.getSessionDate(), s.getWatchedMinutes(), duration, s.isCompleted());
            }
        }
        System.out.println(TABLE_BORDER);
    }

    private void renderSingleSessionTable(StreamingSession s) {
        Movie m = this.movieController.findByIdIncludeDeleted(s.getMovieId());
        int duration = (m == null) ? s.getWatchedMinutes() : m.getDurationMinutes();
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        printSessionRow(s.getSessionId(), s.getSubscriberId(), s.getMovieId(),
                s.getSessionDate(), s.getWatchedMinutes(), duration, s.isCompleted());
        System.out.println(TABLE_BORDER);
    }

    private void printSessionRow(String sessId, String subId, String movId,
            LocalDateTime date, int watched, int duration, boolean complete) {
        System.out.printf("| %-6s | %-6s | %-6s | %-16s | %4d min | %4d min | %-8s |%n",
                TableFormatter.fit(sessId, 6),
                TableFormatter.fit(subId, 6),
                TableFormatter.fit(movId, 6),
                TableFormatter.fit(InputValidator.formatDateTime(date), 16),
                watched,
                duration,
                complete ? "Complete" : "Partial");
    }
}
