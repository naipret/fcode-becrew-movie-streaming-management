package fcode.becrew.moviestreamingmanagement.controller;

import fcode.becrew.moviestreamingmanagement.model.Category;
import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.model.StreamingSession;
import fcode.becrew.moviestreamingmanagement.model.Subscriber;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Controller executing platform analytics, sliding window trending categories,
 * Max-Heap PriorityQueue Top-K ranking, and monthly performance reporting.
 */
public class AnalyticsController {

    private final StreamingSessionController sessionController;
    private final MovieController movieController;
    private final CategoryController categoryController;
    private final SubscriberController subscriberController;

    /**
     * Constructs an AnalyticsController with required collaborators.
     *
     * @param sessionController    collaborator for querying session logs
     * @param movieController      collaborator for querying movies
     * @param categoryController   collaborator for querying categories
     * @param subscriberController collaborator for querying subscribers
     */
    public AnalyticsController(StreamingSessionController sessionController,
            MovieController movieController,
            CategoryController categoryController,
            SubscriberController subscriberController) {
        if (sessionController == null || movieController == null
                || categoryController == null || subscriberController == null) {
            throw new IllegalArgumentException("Collaborators cannot be null.");
        }
        this.sessionController = sessionController;
        this.movieController = movieController;
        this.categoryController = categoryController;
        this.subscriberController = subscriberController;
    }

    /**
     * Aggregates global platform statistics across subscribers, content, and
     * sessions.
     *
     * @return summary PlatformStatistics snapshot
     */
    public PlatformStatistics getPlatformStatistics() {
        List<Subscriber> activeSubscribers = this.subscriberController.getAllActiveSubscribers();
        List<Movie> activeMovies = this.movieController.getAllActiveMovies();
        List<StreamingSession> allSessions = this.sessionController.getAllSessions();

        int totalSessions = allSessions.size();
        long totalMinutes = 0;
        int completedSessions = 0;
        for (int i = 0; i < totalSessions; i++) {
            StreamingSession s = allSessions.get(i);
            totalMinutes += s.getWatchedMinutes();
            if (s.isCompleted()) {
                completedSessions++;
            }
        }

        double totalHours = totalMinutes / 60.0;
        double completionRate = (totalSessions > 0)
                ? (completedSessions * 100.0) / totalSessions
                : 0.0;

        double monthlyRevenue = 0.0;
        for (int i = 0; i < activeSubscribers.size(); i++) {
            monthlyRevenue += activeSubscribers.get(i).calculateMonthlyFee();
        }

        return new PlatformStatistics(
                activeSubscribers.size(),
                activeMovies.size(),
                totalSessions,
                totalHours,
                completionRate,
                monthlyRevenue);
    }

    /**
     * Calculates trending categories within a sliding window relative to current
     * time.
     *
     * @param windowDays the number of days to look back (e.g. 7 or 30)
     * @return sorted list of TrendingCategorySummary records
     */
    public List<TrendingCategorySummary> getTrendingCategories(int windowDays) {
        if (windowDays <= 0) {
            throw new IllegalArgumentException("Trending window days must be greater than zero.");
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(windowDays);
        List<StreamingSession> allSessions = this.sessionController.getAllSessions();

        Map<String, Long> categoryMinutes = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (int i = 0; i < allSessions.size(); i++) {
            StreamingSession s = allSessions.get(i);
            if (s.getSessionDate().isAfter(cutoff)) {
                Movie movie = this.movieController.findByIdIncludeDeleted(s.getMovieId());
                if (movie != null) {
                    String catId = movie.getCategoryId();
                    long currentMin = categoryMinutes.getOrDefault(catId, 0L);
                    categoryMinutes.put(catId, currentMin + s.getWatchedMinutes());

                    int currentCount = categoryCounts.getOrDefault(catId, 0);
                    categoryCounts.put(catId, currentCount + 1);
                }
            }
        }

        List<TrendingCategorySummary> summaries = new ArrayList<>();
        List<Category> categories = this.categoryController.getAllCategories();

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            long minutes = categoryMinutes.getOrDefault(cat.getId(), 0L);
            int count = categoryCounts.getOrDefault(cat.getId(), 0);
            if (count > 0) {
                double hours = minutes / 60.0;
                summaries.add(new TrendingCategorySummary(0, cat.getId(), cat.getName(), hours, count));
            }
        }

        summaries.sort(new Comparator<TrendingCategorySummary>() {
            @Override
            public int compare(TrendingCategorySummary a, TrendingCategorySummary b) {
                return Double.compare(b.getTotalHours(), a.getTotalHours());
            }
        });

        List<TrendingCategorySummary> ranked = new ArrayList<>(summaries.size());
        for (int i = 0; i < summaries.size(); i++) {
            TrendingCategorySummary item = summaries.get(i);
            ranked.add(new TrendingCategorySummary(i + 1, item.getCategoryId(),
                    item.getCategoryName(), item.getTotalHours(), item.getSessionCount()));
        }

        return Collections.unmodifiableList(ranked);
    }

    /**
     * Computes the Top-K ranked movies using a Max-Heap PriorityQueue.
     *
     * @param limit maximum number of movies to return (Top-K)
     * @return sorted list of Top-K MovieRankSummary entries
     */
    public List<MovieRankSummary> getTopRankedMovies(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Ranking limit must be greater than zero.");
        }
        List<Movie> activeMovies = this.movieController.getAllActiveMovies();
        if (activeMovies.isEmpty()) {
            return Collections.emptyList();
        }

        int maxViews = 0;
        for (int i = 0; i < activeMovies.size(); i++) {
            int views = activeMovies.get(i).getViewCount();
            if (views > maxViews) {
                maxViews = views;
            }
        }

        PriorityQueue<MovieRankSummary> maxHeap = new PriorityQueue<>(
                activeMovies.size(),
                new Comparator<MovieRankSummary>() {
                    @Override
                    public int compare(MovieRankSummary a, MovieRankSummary b) {
                        return Double.compare(b.getScore(), a.getScore());
                    }
                });

        for (int i = 0; i < activeMovies.size(); i++) {
            Movie movie = activeMovies.get(i);
            List<StreamingSession> movieSessions = this.sessionController.getSessionsByMovie(movie.getId());
            int completed = 0;
            for (int j = 0; j < movieSessions.size(); j++) {
                if (movieSessions.get(j).isCompleted()) {
                    completed++;
                }
            }

            double completionRatio = (movieSessions.isEmpty())
                    ? 0.5
                    : ((double) completed / movieSessions.size());

            double ratingNorm = movie.getRating() / 10.0;
            double viewNorm = Math.log10(movie.getViewCount() + 1.0)
                    / Math.log10(Math.max(10.0, maxViews + 10.0));

            double score = (0.50 * ratingNorm + 0.30 * viewNorm + 0.20 * completionRatio) * 10.0;

            maxHeap.offer(new MovieRankSummary(0, movie.getId(), movie.getTitle(),
                    movie.getRating(), movie.getViewCount(), score));
        }

        List<MovieRankSummary> results = new ArrayList<>();
        int rank = 1;
        while (!maxHeap.isEmpty() && results.size() < limit) {
            MovieRankSummary item = maxHeap.poll();
            results.add(new MovieRankSummary(rank++, item.getMovieId(), item.getTitle(),
                    item.getRating(), item.getViewCount(), item.getScore()));
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Generates a monthly performance summary report for a given YearMonth.
     *
     * @param month the YearMonth to report on
     * @return MonthlyPerformanceSummary containing aggregated metrics
     */
    public MonthlyPerformanceSummary getMonthlyPerformanceReport(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("YearMonth cannot be null.");
        }
        List<StreamingSession> allSessions = this.sessionController.getAllSessions();
        List<Subscriber> activeSubscribers = this.subscriberController.getAllActiveSubscribers();

        int monthSessions = 0;
        long monthMinutes = 0;
        int completedCount = 0;

        for (int i = 0; i < allSessions.size(); i++) {
            StreamingSession s = allSessions.get(i);
            YearMonth sessionMonth = YearMonth.from(s.getSessionDate());
            if (sessionMonth.equals(month)) {
                monthSessions++;
                monthMinutes += s.getWatchedMinutes();
                if (s.isCompleted()) {
                    completedCount++;
                }
            }
        }

        double monthHours = monthMinutes / 60.0;
        double completionRate = (monthSessions > 0)
                ? (completedCount * 100.0) / monthSessions
                : 0.0;

        double estimatedRevenue = 0.0;
        for (int i = 0; i < activeSubscribers.size(); i++) {
            estimatedRevenue += activeSubscribers.get(i).calculateMonthlyFee();
        }

        return new MonthlyPerformanceSummary(
                month,
                activeSubscribers.size(),
                monthSessions,
                monthHours,
                completionRate,
                estimatedRevenue);
    }

    /**
     * Value record holding global platform metrics.
     */
    public static final class PlatformStatistics {

        private final int activeSubscribers;
        private final int activeMovies;
        private final int totalSessions;
        private final double totalHours;
        private final double completionRate;
        private final double projectedRevenue;

        /**
         * Constructs PlatformStatistics.
         *
         * @param activeSubscribers total active subscribers
         * @param activeMovies      total active movies
         * @param totalSessions     total logged sessions
         * @param totalHours        cumulative hours streamed
         * @param completionRate    overall completion percentage
         * @param projectedRevenue  estimated monthly subscription revenue
         */
        public PlatformStatistics(int activeSubscribers, int activeMovies, int totalSessions,
                double totalHours, double completionRate, double projectedRevenue) {
            this.activeSubscribers = activeSubscribers;
            this.activeMovies = activeMovies;
            this.totalSessions = totalSessions;
            this.totalHours = totalHours;
            this.completionRate = completionRate;
            this.projectedRevenue = projectedRevenue;
        }

        public int getActiveSubscribers() {
            return activeSubscribers;
        }

        public int getActiveMovies() {
            return activeMovies;
        }

        public int getTotalSessions() {
            return totalSessions;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public double getCompletionRate() {
            return completionRate;
        }

        public double getProjectedRevenue() {
            return projectedRevenue;
        }
    }

    /**
     * Value record holding trending category statistics.
     */
    public static final class TrendingCategorySummary {

        private final int rank;
        private final String categoryId;
        private final String categoryName;
        private final double totalHours;
        private final int sessionCount;

        /**
         * Constructs TrendingCategorySummary.
         *
         * @param rank         the rank position
         * @param categoryId   the category identifier
         * @param categoryName the category name
         * @param totalHours   total hours streamed
         * @param sessionCount total sessions recorded
         */
        public TrendingCategorySummary(int rank, String categoryId, String categoryName,
                double totalHours, int sessionCount) {
            this.rank = rank;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.totalHours = totalHours;
            this.sessionCount = sessionCount;
        }

        public int getRank() {
            return rank;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public int getSessionCount() {
            return sessionCount;
        }
    }

    /**
     * Value record holding Top-K ranked movie score details.
     */
    public static final class MovieRankSummary {

        private final int rank;
        private final String movieId;
        private final String title;
        private final double rating;
        private final int viewCount;
        private final double score;

        /**
         * Constructs MovieRankSummary.
         *
         * @param rank      the leaderboard rank
         * @param movieId   the movie identifier
         * @param title     the movie title
         * @param rating    the critic rating
         * @param viewCount the total view count
         * @param score     the weighted normalized score
         */
        public MovieRankSummary(int rank, String movieId, String title,
                double rating, int viewCount, double score) {
            this.rank = rank;
            this.movieId = movieId;
            this.title = title;
            this.rating = rating;
            this.viewCount = viewCount;
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public String getMovieId() {
            return movieId;
        }

        public String getTitle() {
            return title;
        }

        public double getRating() {
            return rating;
        }

        public int getViewCount() {
            return viewCount;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * Value record holding monthly platform performance metrics.
     */
    public static final class MonthlyPerformanceSummary {

        private final YearMonth month;
        private final int activeSubscribers;
        private final int totalSessions;
        private final double totalHours;
        private final double completionRate;
        private final double estimatedRevenue;

        /**
         * Constructs MonthlyPerformanceSummary.
         *
         * @param month             the reported YearMonth
         * @param activeSubscribers total active subscribers
         * @param totalSessions     total sessions in month
         * @param totalHours        total streaming hours
         * @param completionRate    monthly completion percentage
         * @param estimatedRevenue  projected monthly subscription revenue
         */
        public MonthlyPerformanceSummary(YearMonth month, int activeSubscribers, int totalSessions,
                double totalHours, double completionRate, double estimatedRevenue) {
            this.month = month;
            this.activeSubscribers = activeSubscribers;
            this.totalSessions = totalSessions;
            this.totalHours = totalHours;
            this.completionRate = completionRate;
            this.estimatedRevenue = estimatedRevenue;
        }

        public YearMonth getMonth() {
            return month;
        }

        public int getActiveSubscribers() {
            return activeSubscribers;
        }

        public int getTotalSessions() {
            return totalSessions;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public double getCompletionRate() {
            return completionRate;
        }

        public double getEstimatedRevenue() {
            return estimatedRevenue;
        }
    }
}
