package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.controller.CategoryController;
import fcode.becrew.moviestreamingmanagement.controller.MovieController;
import fcode.becrew.moviestreamingmanagement.model.Category;
import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import fcode.becrew.moviestreamingmanagement.util.TableFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Interactive 80-column console menu for managing the movie catalog:
 * adding movies, viewing with integrated keyword search, multi-condition
 * predicate filtering, sorting, inspecting detail cards, updating, and soft-deleting.
 */
public class MovieMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Add New Movie",
            "View All Movies",
            "Update Movie",
            "Remove Movie"
    };

    private static final String TABLE_BORDER =
            "+--------+--------------------------+--------------+------+---------+--------+";
    private static final String TABLE_HEADER =
            "| ID     | Title                    | Category     | Year | Dur     | Rating |";

    private final MovieController movieController;
    private final CategoryController categoryController;

    /**
     * Constructs a MovieMenu.
     *
     * @param movieController    collaborator for movie catalog operations
     * @param categoryController collaborator for category queries
     */
    public MovieMenu(MovieController movieController, CategoryController categoryController) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM > [1] MOVIES", MENU_OPTIONS);
        if (movieController == null || categoryController == null) {
            throw new IllegalArgumentException("Controllers cannot be null.");
        }
        this.movieController = movieController;
        this.categoryController = categoryController;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                handleAddMovie();
                break;
            case 2:
                handleViewAllMovies();
                break;
            case 3:
                handleUpdateMovie();
                break;
            case 4:
                handleRemoveMovie();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    private void handleAddMovie() {
        while (true) {
            String title = ConsoleUtil.readString("Enter movie title (or 0 to return): ");
            if (title == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            String catId = ConsoleUtil.readString("Enter Category ID (or 0 to cancel): ");
            if (catId == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            Category category = this.categoryController.findById(catId);
            if (category == null) {
                System.out.println("[ERROR] Category not found with ID: " + catId);
                printDivider();
                continue;
            }

            String director = ConsoleUtil.readString("Enter director name (or 0 to cancel): ");
            if (director == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            String cast = ConsoleUtil.readOptionalString("Enter cast members (comma-separated, or Enter to skip): ");
            if ("0".equals(cast)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            int currentYear = LocalDate.now().getYear();
            Integer releaseYear = ConsoleUtil.readOptionalInt(
                    "Enter release year (1888-" + (currentYear + 5) + ") [Enter = " + currentYear + "]: ",
                    1888, currentYear + 5, currentYear);
            if (releaseYear == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            Integer durationMinutes = ConsoleUtil.readOptionalInt(
                    "Enter runtime duration in minutes [Enter = 120]: ",
                    1, 1000, 120);
            if (durationMinutes == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            Double rating = ConsoleUtil.readOptionalDouble(
                    "Enter critic rating (0.0 - 10.0) [Enter = 7.0]: ",
                    0.0, 10.0, 7.0);
            if (rating == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            System.out.println();
            System.out.println(TABLE_BORDER);
            System.out.println(TABLE_HEADER);
            System.out.println(TABLE_BORDER);
            printMovieRow("PREVIEW", title, category.getName(), releaseYear, durationMinutes, rating);
            System.out.println(TABLE_BORDER);

            if (!ConsoleUtil.confirm("Confirm adding this movie to the catalog?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                Movie movie = this.movieController.addMovie(title, category.getId(), director,
                        cast, releaseYear, durationMinutes, rating);
                System.out.println("[OK] Movie " + movie.getId() + " added successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleViewAllMovies() {
        Predicate<Movie> activeFilter = null;
        Comparator<Movie> activeSort = null;
        String filterDesc = "None";
        String sortDesc = "Default (ID Ascending)";

        while (true) {
            List<Movie> displayList = (activeFilter == null)
                    ? new ArrayList<>(this.movieController.getAllActiveMovies())
                    : new ArrayList<>(this.movieController.filter(activeFilter));

            if (activeSort != null) {
                displayList.sort(activeSort);
            }

            System.out.println();
            System.out.println("  Active Filter : " + filterDesc);
            System.out.println("  Active Sort   : " + sortDesc);
            renderMovieTable(displayList);

            System.out.println("Commands: [Keyword to search] | [Movie ID to inspect] | [F] Filter | [S] Sort | [R] Reset | [0] Return");
            String input = ConsoleUtil.readOptionalString("Enter command: ");
            if ("0".equals(input)) {
                return;
            }
            if (input.isEmpty()) {
                continue;
            }

            String cmd = input.toUpperCase(Locale.US);
            if ("R".equals(cmd)) {
                activeFilter = null;
                activeSort = null;
                filterDesc = "None";
                sortDesc = "Default (ID Ascending)";
                System.out.println("[INFO] Reset filters and sorting.");
                continue;
            }

            if ("S".equals(cmd)) {
                System.out.println();
                System.out.println("  [1] Title (A - Z)");
                System.out.println("  [2] Rating (Highest First)");
                System.out.println("  [3] Release Year (Newest First)");
                System.out.println("  [4] Total Views (Most Viewed First)");
                System.out.println("  [0] Cancel");
                int sortChoice = ConsoleUtil.readInt("Select sort criteria [0-4]: ", 0, 4);
                switch (sortChoice) {
                    case 1:
                        activeSort = new Comparator<Movie>() {
                            @Override
                            public int compare(Movie a, Movie b) {
                                return a.getTitle().compareToIgnoreCase(b.getTitle());
                            }
                        };
                        sortDesc = "Title (A - Z)";
                        break;
                    case 2:
                        activeSort = new Comparator<Movie>() {
                            @Override
                            public int compare(Movie a, Movie b) {
                                return Double.compare(b.getRating(), a.getRating());
                            }
                        };
                        sortDesc = "Rating (Highest First)";
                        break;
                    case 3:
                        activeSort = new Comparator<Movie>() {
                            @Override
                            public int compare(Movie a, Movie b) {
                                return Integer.compare(b.getReleaseYear(), a.getReleaseYear());
                            }
                        };
                        sortDesc = "Release Year (Newest First)";
                        break;
                    case 4:
                        activeSort = new Comparator<Movie>() {
                            @Override
                            public int compare(Movie a, Movie b) {
                                return Integer.compare(b.getViewCount(), a.getViewCount());
                            }
                        };
                        sortDesc = "Total Views (Most Viewed First)";
                        break;
                    default:
                        break;
                }
                continue;
            }

            if ("F".equals(cmd)) {
                System.out.println();
                System.out.println("  Configure Multi-Condition Movie Filter (Enter to skip condition):");
                String catQuery = ConsoleUtil.readOptionalString("  Category ID or Name: ");
                Double minRating = ConsoleUtil.readOptionalDouble("  Minimum Rating (0.0-10.0): ", 0.0, 10.0, null);
                Integer yearFrom = ConsoleUtil.readOptionalInt("  Release Year From: ", 1888, 2050, null);
                Integer yearTo = ConsoleUtil.readOptionalInt("  Release Year To: ", 1888, 2050, null);
                Integer maxDuration = ConsoleUtil.readOptionalInt("  Maximum Duration (minutes): ", 1, 1000, null);

                List<Predicate<Movie>> predicates = new ArrayList<>();
                StringBuilder descBuilder = new StringBuilder();

                if (!catQuery.isEmpty()) {
                    predicates.add(new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            Category c = categoryController.findById(m.getCategoryId());
                            return (c != null && c.getName().toLowerCase().contains(catQuery.toLowerCase()))
                                    || m.getCategoryId().equalsIgnoreCase(catQuery);
                        }
                    });
                    descBuilder.append("Category contains '").append(catQuery).append("' ");
                }
                if (minRating != null) {
                    predicates.add(new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            return m.getRating() >= minRating;
                        }
                    });
                    descBuilder.append("Rating >= ").append(minRating).append(" ");
                }
                if (yearFrom != null) {
                    predicates.add(new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            return m.getReleaseYear() >= yearFrom;
                        }
                    });
                    descBuilder.append("Year >= ").append(yearFrom).append(" ");
                }
                if (yearTo != null) {
                    predicates.add(new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            return m.getReleaseYear() <= yearTo;
                        }
                    });
                    descBuilder.append("Year <= ").append(yearTo).append(" ");
                }
                if (maxDuration != null) {
                    predicates.add(new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            return m.getDurationMinutes() <= maxDuration;
                        }
                    });
                    descBuilder.append("Duration <= ").append(maxDuration).append("min ");
                }

                if (predicates.isEmpty()) {
                    activeFilter = null;
                    filterDesc = "None";
                } else {
                    activeFilter = new Predicate<Movie>() {
                        @Override
                        public boolean test(Movie m) {
                            for (int i = 0; i < predicates.size(); i++) {
                                if (!predicates.get(i).test(m)) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    };
                    filterDesc = descBuilder.toString().trim();
                }
                continue;
            }

            Movie targetMovie = this.movieController.findById(input);
            if (targetMovie != null) {
                renderMovieDetailCard(targetMovie);
                continue;
            }

            List<Movie> searchResults = this.movieController.search(input);
            if (searchResults.isEmpty()) {
                System.out.println("[INFO] No active movies found matching keyword '" + input + "'.");
            } else {
                System.out.println();
                System.out.println("  Search Results for: '" + input + "'");
                renderMovieTable(searchResults);
            }
        }
    }

    private void handleUpdateMovie() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Movie ID to update (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Movie movie = this.movieController.findById(rawId);
            if (movie == null) {
                System.out.println("[ERROR] Active movie not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            System.out.println("CURRENT MOVIE DETAILS:");
            renderSingleMovieTable(movie);

            System.out.println("Press [Enter] to keep current value, or type 0 to cancel:");
            String newTitle = ConsoleUtil.readOptionalString("Title [" + movie.getTitle() + "]: ");
            if ("0".equals(newTitle)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newTitle.isEmpty()) {
                newTitle = movie.getTitle();
            }

            String newCatId = ConsoleUtil.readOptionalString("Category ID [" + movie.getCategoryId() + "]: ");
            if ("0".equals(newCatId)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newCatId.isEmpty()) {
                newCatId = movie.getCategoryId();
            }
            Category category = this.categoryController.findById(newCatId);
            if (category == null) {
                System.out.println("[ERROR] Category not found with ID: " + newCatId);
                printDivider();
                continue;
            }

            String newDirector = ConsoleUtil.readOptionalString("Director [" + movie.getDirector() + "]: ");
            if ("0".equals(newDirector)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newDirector.isEmpty()) {
                newDirector = movie.getDirector();
            }

            String newCast = ConsoleUtil.readOptionalString("Cast [" + movie.getCast() + "]: ");
            if ("0".equals(newCast)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newCast.isEmpty()) {
                newCast = movie.getCast();
            }

            Integer newYear = ConsoleUtil.readOptionalInt("Release Year [" + movie.getReleaseYear() + "]: ",
                    1888, 2050, movie.getReleaseYear());
            if (newYear == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            Integer newDuration = ConsoleUtil.readOptionalInt("Duration [" + movie.getDurationMinutes() + " min]: ",
                    1, 1000, movie.getDurationMinutes());
            if (newDuration == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            Double newRating = ConsoleUtil.readOptionalDouble("Rating [" + movie.getRating() + "]: ",
                    0.0, 10.0, movie.getRating());
            if (newRating == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            if (!ConsoleUtil.confirm("Confirm updating movie " + movie.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.movieController.updateMovie(movie.getId(), newTitle, category.getId(),
                        newDirector, newCast, newYear, newDuration, newRating);
                System.out.println("[OK] Movie " + movie.getId() + " updated successfully.");
                renderSingleMovieTable(movie);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleRemoveMovie() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Movie ID to remove (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Movie movie = this.movieController.findById(rawId);
            if (movie == null) {
                System.out.println("[ERROR] Active movie not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleMovieTable(movie);

            if (!ConsoleUtil.confirm("Are you sure you want to remove movie " + movie.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.movieController.deleteMovie(movie.getId());
                System.out.println("[OK] Movie " + movie.getId() + " soft-deleted successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void renderMovieTable(List<Movie> movieList) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        if (movieList.isEmpty()) {
            System.out.println("|" + TableFormatter.center("No active movies found.", TableFormatter.SCREEN_WIDTH - 2) + "|");
        } else {
            for (int i = 0; i < movieList.size(); i++) {
                Movie m = movieList.get(i);
                Category cat = this.categoryController.findByIdIncludeDeleted(m.getCategoryId());
                String catName = (cat == null) ? m.getCategoryId() : cat.getName();
                printMovieRow(m.getId(), m.getTitle(), catName, m.getReleaseYear(), m.getDurationMinutes(), m.getRating());
            }
        }
        System.out.println(TABLE_BORDER);
    }

    private void renderSingleMovieTable(Movie movie) {
        Category cat = this.categoryController.findByIdIncludeDeleted(movie.getCategoryId());
        String catName = (cat == null) ? movie.getCategoryId() : cat.getName();
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        printMovieRow(movie.getId(), movie.getTitle(), catName, movie.getReleaseYear(),
                movie.getDurationMinutes(), movie.getRating());
        System.out.println(TABLE_BORDER);
    }

    private void printMovieRow(String id, String title, String category, int year, int duration, double rating) {
        System.out.printf("| %-6s | %-24s | %-12s | %4d | %4d min | %6.1f |%n",
                TableFormatter.fit(id, 6),
                TableFormatter.fit(title, 24),
                TableFormatter.fit(category, 12),
                year,
                duration,
                rating);
    }

    private void renderMovieDetailCard(Movie movie) {
        Category cat = this.categoryController.findByIdIncludeDeleted(movie.getCategoryId());
        String catName = (cat == null) ? movie.getCategoryId() : cat.getName();
        String border = "+" + TableFormatter.repeat('-', TableFormatter.SCREEN_WIDTH - 2) + "+";

        System.out.println();
        System.out.println(border);
        System.out.println("| " + TableFormatter.fit("MOVIE DETAILS: " + movie.getId() + " - " + movie.getTitle(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);
        System.out.println("| " + TableFormatter.fit("Category   : " + catName + " (" + movie.getCategoryId() + ")", TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Director   : " + movie.getDirector(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Cast       : " + movie.getCast(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Released   : " + movie.getReleaseYear(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Duration   : " + movie.getDurationMinutes() + " minutes", TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Rating     : " + String.format(Locale.US, "%.1f / 10.0", movie.getRating()), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Total Views: " + String.format(Locale.US, "%,d views", movie.getViewCount()), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Status     : " + (movie.isDeleted() ? "Soft-Deleted" : "Active"), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);
    }
}
