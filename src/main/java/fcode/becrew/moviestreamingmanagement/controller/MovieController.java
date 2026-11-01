package fcode.becrew.moviestreamingmanagement.controller;

import fcode.becrew.moviestreamingmanagement.model.Category;
import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.util.CsvUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Controller coordinating movie catalog management, multi-criteria predicate filtering,
 * universal keyword search, category referential integrity, and atomic CSV persistence.
 */
public class MovieController {

    private static final String CSV_HEADER =
            "id,title,categoryId,director,cast,releaseYear,durationMinutes,rating,viewCount,isDeleted";

    private final Path csvPath;
    private final List<Movie> movies;
    private final CategoryController categoryController;

    /**
     * Constructs a MovieController.
     *
     * @param csvPath            the filesystem Path to data/movies.csv
     * @param categoryController collaborator for validating category references
     */
    public MovieController(Path csvPath, CategoryController categoryController) {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path cannot be null.");
        }
        if (categoryController == null) {
            throw new IllegalArgumentException("CategoryController cannot be null.");
        }
        this.csvPath = csvPath;
        this.categoryController = categoryController;
        this.movies = new ArrayList<>();
        loadFromCsv();
    }

    /**
     * Loads movie records from persistent CSV storage into memory.
     */
    public final void loadFromCsv() {
        this.movies.clear();
        List<String[]> rows = CsvUtil.readCsv(this.csvPath);
        if (rows.isEmpty()) {
            return;
        }

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 10) {
                continue;
            }
            try {
                String id = row[0].trim();
                String title = row[1].trim();
                String categoryId = row[2].trim();
                String director = row[3].trim();
                String cast = row[4].trim();
                int releaseYear = Integer.parseInt(row[5].trim());
                int durationMinutes = Integer.parseInt(row[6].trim());
                double rating = Double.parseDouble(row[7].trim());
                int viewCount = Integer.parseInt(row[8].trim());
                boolean isDeleted = Boolean.parseBoolean(row[9].trim());

                this.movies.add(new Movie(id, title, categoryId, director, cast,
                        releaseYear, durationMinutes, rating, viewCount, isDeleted));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Persists all in-memory movie records atomically to the CSV file.
     */
    public void saveToCsv() {
        List<String[]> rows = new ArrayList<>(this.movies.size() + 1);
        rows.add(CSV_HEADER.split(","));
        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            rows.add(new String[]{
                    movie.getId(),
                    movie.getTitle(),
                    movie.getCategoryId(),
                    movie.getDirector(),
                    movie.getCast(),
                    Integer.toString(movie.getReleaseYear()),
                    Integer.toString(movie.getDurationMinutes()),
                    Double.toString(movie.getRating()),
                    Integer.toString(movie.getViewCount()),
                    Boolean.toString(movie.isDeleted())
            });
        }
        CsvUtil.writeToFile(this.csvPath, rows);
    }

    /**
     * Adds a new movie to the catalog after validating metadata and category existence.
     *
     * @param title           the movie title
     * @param categoryId      the referenced category identifier
     * @param director        the director name
     * @param cast            the cast members list
     * @param releaseYear     the release year
     * @param durationMinutes total duration in minutes
     * @param rating          critic rating (0.0 to 10.0)
     * @return the created Movie entity
     * @throws IllegalArgumentException if title already exists or category is invalid
     */
    public Movie addMovie(String title, String categoryId, String director,
                          String cast, int releaseYear, int durationMinutes, double rating) {
        String cleanTitle = InputValidator.normalizeText(title, "Movie title");
        Movie existing = findByTitle(cleanTitle);
        if (existing != null) {
            throw new IllegalArgumentException("Active movie with title '" + cleanTitle + "' already exists.");
        }

        Category category = this.categoryController.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Referenced category does not exist: " + categoryId);
        }

        String nextId = generateNextId();
        Movie movie = new Movie(nextId, cleanTitle, category.getId(), director,
                cast, releaseYear, durationMinutes, rating, 0, false);
        this.movies.add(movie);
        saveToCsv();
        return movie;
    }

    /**
     * Updates an existing movie record.
     *
     * @param id              the movie identifier
     * @param title           the updated title
     * @param categoryId      the updated category identifier
     * @param director        the updated director
     * @param cast            the updated cast
     * @param releaseYear     the updated release year
     * @param durationMinutes the updated duration
     * @param rating          the updated rating
     * @throws IllegalArgumentException if movie not found or title conflicts
     */
    public void updateMovie(String id, String title, String categoryId, String director,
                            String cast, int releaseYear, int durationMinutes, double rating) {
        Movie movie = findById(id);
        if (movie == null) {
            throw new IllegalArgumentException("Movie not found with ID: " + id);
        }

        String cleanTitle = InputValidator.normalizeText(title, "Movie title");
        if (!movie.getTitle().equalsIgnoreCase(cleanTitle)) {
            Movie existing = findByTitle(cleanTitle);
            if (existing != null && !existing.getId().equals(movie.getId())) {
                throw new IllegalArgumentException("Active movie with title '" + cleanTitle + "' already exists.");
            }
        }

        Category category = this.categoryController.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Referenced category does not exist: " + categoryId);
        }

        movie.setTitle(cleanTitle);
        movie.setCategoryId(category.getId());
        movie.setDirector(director);
        movie.setCast(cast);
        movie.setReleaseYear(releaseYear);
        movie.setDurationMinutes(durationMinutes);
        movie.setRating(rating);
        saveToCsv();
    }

    /**
     * Soft-deletes a movie from the active catalog.
     *
     * @param id the movie identifier to soft-delete
     * @throws IllegalArgumentException if the movie is not found
     */
    public void deleteMovie(String id) {
        Movie movie = findById(id);
        if (movie == null) {
            throw new IllegalArgumentException("Movie not found with ID: " + id);
        }
        movie.setDeleted(true);
        saveToCsv();
    }

    /**
     * Finds an active movie by identifier.
     *
     * @param id the identifier string, may be shorthand
     * @return the active Movie, or null if nonexistent or soft-deleted
     */
    public Movie findById(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'M');
            for (int i = 0; i < this.movies.size(); i++) {
                Movie movie = this.movies.get(i);
                if (movie.getId().equalsIgnoreCase(canonicalId) && !movie.isDeleted()) {
                    return movie;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds a movie by identifier regardless of soft-delete status.
     *
     * @param id the identifier string
     * @return the Movie entity, or null if nonexistent
     */
    public Movie findByIdIncludeDeleted(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'M');
            for (int i = 0; i < this.movies.size(); i++) {
                Movie movie = this.movies.get(i);
                if (movie.getId().equalsIgnoreCase(canonicalId)) {
                    return movie;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds an active movie by case-insensitive title match.
     *
     * @param title the movie title to search
     * @return matching active Movie, or null if none found
     */
    public Movie findByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        String clean = title.trim();
        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            if (!movie.isDeleted() && movie.getTitle().equalsIgnoreCase(clean)) {
                return movie;
            }
        }
        return null;
    }

    /**
     * Returns whether any active movie currently references the specified category.
     *
     * @param categoryId the category identifier to check
     * @return true if active movies reference this category, false otherwise
     */
    public boolean hasActiveMoviesForCategory(String categoryId) {
        if (categoryId == null) {
            return false;
        }
        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            if (!movie.isDeleted() && movie.getCategoryId().equalsIgnoreCase(categoryId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches active movies matching a keyword across title, director, cast, or category name.
     *
     * @param keyword the search query keyword
     * @return list of matching active Movie entities
     */
    public List<Movie> search(String keyword) {
        List<Movie> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveMovies();
        }
        String query = keyword.trim().toLowerCase(Locale.US);

        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            if (movie.isDeleted()) {
                continue;
            }
            if (movie.getId().toLowerCase(Locale.US).contains(query)
                    || movie.getTitle().toLowerCase(Locale.US).contains(query)
                    || movie.getDirector().toLowerCase(Locale.US).contains(query)
                    || movie.getCast().toLowerCase(Locale.US).contains(query)) {
                results.add(movie);
                continue;
            }
            Category cat = this.categoryController.findById(movie.getCategoryId());
            if (cat != null && cat.getName().toLowerCase(Locale.US).contains(query)) {
                results.add(movie);
            }
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Evaluates a dynamic specification predicate against all active movies.
     *
     * @param criteria the composite predicate filter
     * @return list of active movies satisfying the criteria
     */
    public List<Movie> filter(Predicate<Movie> criteria) {
        List<Movie> results = new ArrayList<>();
        if (criteria == null) {
            return getAllActiveMovies();
        }
        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            if (!movie.isDeleted() && criteria.test(movie)) {
                results.add(movie);
            }
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Returns an unmodifiable list of all active movies.
     *
     * @return unmodifiable list of active Movie entities
     */
    public List<Movie> getAllActiveMovies() {
        List<Movie> active = new ArrayList<>();
        for (int i = 0; i < this.movies.size(); i++) {
            Movie movie = this.movies.get(i);
            if (!movie.isDeleted()) {
                active.add(movie);
            }
        }
        return Collections.unmodifiableList(active);
    }

    /**
     * Returns an unmodifiable list of all movie records including soft-deleted ones.
     *
     * @return unmodifiable list of all Movie entities
     */
    public List<Movie> getAllMovies() {
        return Collections.unmodifiableList(new ArrayList<>(this.movies));
    }

    private String generateNextId() {
        int max = 0;
        for (int i = 0; i < this.movies.size(); i++) {
            String rawId = this.movies.get(i).getId();
            if (rawId.length() >= 2 && rawId.charAt(0) == 'M') {
                try {
                    int num = Integer.parseInt(rawId.substring(1));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format(Locale.US, "M%05d", max + 1);
    }
}
