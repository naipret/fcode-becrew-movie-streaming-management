package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController;
import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController.MonthlyPerformanceSummary;
import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController.MovieRankSummary;
import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController.PlatformStatistics;
import fcode.becrew.moviestreamingmanagement.controller.AnalyticsController.TrendingCategorySummary;
import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;
import fcode.becrew.moviestreamingmanagement.util.TableFormatter;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Interactive 80-column console menu for viewing analytical platform reports:
 * platform viewing statistics, sliding window trending categories, Max-Heap
 * Top-K
 * movie rankings, and monthly performance reports.
 */
public class AnalyticsMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Platform Viewing Statistics",
            "Trending Categories Report",
            "Top-K Ranked Movies",
            "Monthly Performance Report"
    };

    private static final DateTimeFormatter MONTH_HEADER_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    private final AnalyticsController analyticsController;

    /**
     * Constructs an AnalyticsMenu.
     *
     * @param analyticsController collaborator for platform analytics algorithms
     */
    public AnalyticsMenu(AnalyticsController analyticsController) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM > [5] REPORTS", MENU_OPTIONS);
        if (analyticsController == null) {
            throw new IllegalArgumentException("AnalyticsController cannot be null.");
        }
        this.analyticsController = analyticsController;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                handlePlatformStatistics();
                break;
            case 2:
                handleTrendingCategories();
                break;
            case 3:
                handleTopRankedMovies();
                break;
            case 4:
                handleMonthlyPerformance();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    private void handlePlatformStatistics() {
        PlatformStatistics stats = this.analyticsController.getPlatformStatistics();
        String border = "+" + TableFormatter.repeat('-', TableFormatter.SCREEN_WIDTH - 2) + "+";

        System.out.println();
        System.out.println(border);
        System.out.println(
                "| " + TableFormatter.center("PLATFORM VIEWING STATISTICS OVERVIEW", TableFormatter.SCREEN_WIDTH - 4)
                        + " |");
        System.out.println(border);
        System.out.println(
                "| " + TableFormatter.fit("Active Subscribers        : " + stats.getActiveSubscribers() + " accounts",
                        TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out
                .println("| " + TableFormatter.fit("Active Movies in Catalog  : " + stats.getActiveMovies() + " movies",
                        TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(
                "| " + TableFormatter.fit("Total Sessions Logged     : " + stats.getTotalSessions() + " sessions",
                        TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit(
                "Total Streaming Time      : " + String.format(Locale.US, "%.1f hours", stats.getTotalHours()),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit(
                "Overall Completion Rate   : " + String.format(Locale.US, "%.1f %%", stats.getCompletionRate()),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit(
                "Projected Monthly Revenue : " + String.format(Locale.US, "$ %,.2f", stats.getProjectedRevenue()),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);

        ConsoleUtil.readOptionalString("Press [Enter] to return to Reports Menu: ");
    }

    private void handleTrendingCategories() {
        while (true) {
            Integer windowDays = ConsoleUtil.readOptionalInt(
                    "Enter trending sliding window in days [Enter = 7, 0 to return]: ",
                    1, 365, 7);
            if (windowDays == null) {
                return;
            }

            List<TrendingCategorySummary> summaries = this.analyticsController.getTrendingCategories(windowDays);

            String border = "+------+--------+-------------------------+------------------+-----------------+";
            String header = "| Rank | CatID  | Category Name           | Total Hours      | Total Sessions  |";

            System.out.println();
            System.out.println("  TRENDING CATEGORIES - LAST " + windowDays + " DAYS");
            System.out.println(border);
            System.out.println(header);
            System.out.println(border);

            if (summaries.isEmpty()) {
                System.out.println("|" + TableFormatter.center("No streaming sessions logged within this time window.",
                        TableFormatter.SCREEN_WIDTH - 2) + "|");
            } else {
                for (int i = 0; i < summaries.size(); i++) {
                    TrendingCategorySummary item = summaries.get(i);
                    System.out.printf("| %4d | %-6s | %-23s | %12.1f hrs | %15d |%n",
                            item.getRank(),
                            TableFormatter.fit(item.getCategoryId(), 6),
                            TableFormatter.fit(item.getCategoryName(), 23),
                            item.getTotalHours(),
                            item.getSessionCount());
                }
            }
            System.out.println(border);
            printDivider();
        }
    }

    private void handleTopRankedMovies() {
        while (true) {
            Integer limit = ConsoleUtil.readOptionalInt(
                    "Enter Top-K movie ranking limit [Enter = 10, 0 to return]: ",
                    1, 100, 10);
            if (limit == null) {
                return;
            }

            List<MovieRankSummary> ranked = this.analyticsController.getTopRankedMovies(limit);

            String border = "+------+--------+---------------------------+--------+------------+------------+";
            String header = "| Rank | ID     | Title                     | Rating | Views      | Score      |";

            System.out.println();
            System.out.println("  TOP-" + limit + " RANKED MOVIES (WEIGHTED MAX-HEAP SCORING)");
            System.out.println(border);
            System.out.println(header);
            System.out.println(border);

            if (ranked.isEmpty()) {
                System.out.println("|" + TableFormatter.center("No active movies available for ranking.",
                        TableFormatter.SCREEN_WIDTH - 2) + "|");
            } else {
                for (int i = 0; i < ranked.size(); i++) {
                    MovieRankSummary item = ranked.get(i);
                    System.out.printf("| %4d | %-6s | %-25s | %6.1f | %10s | %10.3f |%n",
                            item.getRank(),
                            TableFormatter.fit(item.getMovieId(), 6),
                            TableFormatter.fit(item.getTitle(), 25),
                            item.getRating(),
                            TableFormatter.fitRight(String.format(Locale.US, "%,d", item.getViewCount()), 10),
                            item.getScore());
                }
            }
            System.out.println(border);
            printDivider();
        }
    }

    private void handleMonthlyPerformance() {
        while (true) {
            YearMonth targetMonth = ConsoleUtil.readYearMonth(
                    "Enter target month (MM/yyyy) [Enter = current month, 0 to return]: ",
                    true);
            if (targetMonth == null) {
                return;
            }

            MonthlyPerformanceSummary report = this.analyticsController.getMonthlyPerformanceReport(targetMonth);
            String title = "MONTHLY PERFORMANCE REPORT - "
                    + targetMonth.format(MONTH_HEADER_FORMAT).toUpperCase(Locale.US);
            String border = "+" + TableFormatter.repeat('-', TableFormatter.SCREEN_WIDTH - 2) + "+";

            System.out.println();
            System.out.println(border);
            System.out.println("| " + TableFormatter.center(title, TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println(border);
            System.out.println("| " + TableFormatter.fit(
                    "Target Billing Month             : " + targetMonth.format(MONTH_HEADER_FORMAT),
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println("| " + TableFormatter.fit(
                    "Total Active Subscribers         : " + report.getActiveSubscribers() + " subscribers",
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println("| " + TableFormatter.fit(
                    "Total Sessions Recorded in Month : " + report.getTotalSessions() + " sessions",
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println("| " + TableFormatter.fit(
                    "Total Monthly Streaming Time     : "
                            + String.format(Locale.US, "%.1f hours", report.getTotalHours()),
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println("| " + TableFormatter.fit(
                    "Monthly Completion Rate          : "
                            + String.format(Locale.US, "%.1f %%", report.getCompletionRate()),
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println("| " + TableFormatter.fit(
                    "Estimated Monthly Subscription Rev: "
                            + String.format(Locale.US, "$ %,.2f", report.getEstimatedRevenue()),
                    TableFormatter.SCREEN_WIDTH - 4) + " |");
            System.out.println(border);

            printDivider();
        }
    }
}
