package fcode.becrew.moviestreamingmanagement;

import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController;
import fcode.becrew.moviestreamingmanagement.controller.CategoryController;
import fcode.becrew.moviestreamingmanagement.controller.MovieController;
import fcode.becrew.moviestreamingmanagement.controller.StreamingSessionController;
import fcode.becrew.moviestreamingmanagement.controller.SubscriberController;
import fcode.becrew.moviestreamingmanagement.util.CsvUtil;
import fcode.becrew.moviestreamingmanagement.view.AnalyticsMenu;
import fcode.becrew.moviestreamingmanagement.view.CategoryMenu;
import fcode.becrew.moviestreamingmanagement.view.MainMenu;
import fcode.becrew.moviestreamingmanagement.view.MovieMenu;
import fcode.becrew.moviestreamingmanagement.view.StreamingSessionMenu;
import fcode.becrew.moviestreamingmanagement.view.SubscriberMenu;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Main application bootstrap entry point for the Movie Streaming Management System.
 * Constructs dependency injection graph across controllers and views and starts
 * the interactive console lifecycle.
 */
public final class Main {

    private static final String DATA_DIR = "data";
    private static final String CATEGORIES_CSV = "categories.csv";
    private static final String MOVIES_CSV = "movies.csv";
    private static final String SUBSCRIBERS_CSV = "subscribers.csv";
    private static final String SESSIONS_CSV = "sessions.csv";

    private Main() {
        throw new AssertionError("Main cannot be instantiated.");
    }

    /**
     * Application entry point starting the interactive console application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Path dataDir = Paths.get(DATA_DIR);
        ensureDataDirectoryAndFiles(dataDir);

        Path categoriesPath = dataDir.resolve(CATEGORIES_CSV);
        Path moviesPath = dataDir.resolve(MOVIES_CSV);
        Path subscribersPath = dataDir.resolve(SUBSCRIBERS_CSV);
        Path sessionsPath = dataDir.resolve(SESSIONS_CSV);

        CategoryController categoryController = new CategoryController(categoriesPath);
        MovieController movieController = new MovieController(moviesPath, categoryController);
        SubscriberController subscriberController = new SubscriberController(subscribersPath);
        StreamingSessionController sessionController =
                new StreamingSessionController(sessionsPath, subscriberController, movieController);
        AnalyticsController analyticsController =
                new AnalyticsController(sessionController, movieController, categoryController, subscriberController);

        MovieMenu movieMenu = new MovieMenu(movieController, categoryController);
        CategoryMenu categoryMenu = new CategoryMenu(categoryController, movieController);
        SubscriberMenu subscriberMenu = new SubscriberMenu(subscriberController, sessionController);
        StreamingSessionMenu streamingSessionMenu =
                new StreamingSessionMenu(sessionController, subscriberController, movieController);
        AnalyticsMenu analyticsMenu = new AnalyticsMenu(analyticsController);

        MainMenu mainMenu = new MainMenu(
                movieMenu,
                categoryMenu,
                subscriberMenu,
                streamingSessionMenu,
                analyticsMenu
        );

        mainMenu.display();
    }

    private static void ensureDataDirectoryAndFiles(Path dataDir) {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            ensureCsvFile(dataDir.resolve(CATEGORIES_CSV), "id,name,description,isDeleted");
            ensureCsvFile(dataDir.resolve(MOVIES_CSV),
                    "id,title,categoryId,director,cast,releaseYear,durationMinutes,rating,viewCount,isDeleted");
            ensureCsvFile(dataDir.resolve(SUBSCRIBERS_CSV), "id,fullName,phone,email,tier,isDeleted");
            ensureCsvFile(dataDir.resolve(SESSIONS_CSV),
                    "sessionId,subscriberId,movieId,sessionDate,watchedMinutes,isCompleted");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize persistent storage directory: " + dataDir, e);
        }
    }

    private static void ensureCsvFile(Path filePath, String headerLine) {
        if (!Files.exists(filePath)) {
            CsvUtil.writeToFile(filePath, Collections.singletonList(headerLine.split(",")));
        }
    }
}
