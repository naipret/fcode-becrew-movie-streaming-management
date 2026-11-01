package fcode.becrew.moviestreamingmanagement.model;

import fcode.becrew.moviestreamingmanagement.util.InputValidator;

/**
 * Domain entity representing a motion picture in the digital streaming catalog.
 */
public class Movie {

    private final String id;
    private String title;
    private String categoryId;
    private String director;
    private String cast;
    private int releaseYear;
    private int durationMinutes;
    private double rating;
    private int viewCount;
    private boolean isDeleted;

    /**
     * Constructs a Movie domain entity.
     *
     * @param id              the unique movie identifier (e.g. M00001)
     * @param title           the movie title
     * @param categoryId      the referenced category identifier (e.g. G00001)
     * @param director        the director name
     * @param cast            the principal cast members
     * @param releaseYear     the release year
     * @param durationMinutes total duration in minutes
     * @param rating          critic rating between 0.0 and 10.0
     * @param viewCount       cumulative playback session count
     * @param isDeleted       true if soft-deleted, false if active
     */
    public Movie(String id, String title, String categoryId, String director,
                 String cast, int releaseYear, int durationMinutes,
                 double rating, int viewCount, boolean isDeleted) {
        this.id = InputValidator.normalizeId(id, 'M');
        this.title = InputValidator.normalizeText(title, "Movie title");
        this.categoryId = InputValidator.normalizeId(categoryId, 'G');
        this.director = InputValidator.normalizeText(director, "Director");
        this.cast = (cast == null) ? "" : cast.trim();
        InputValidator.validateReleaseYear(releaseYear);
        this.releaseYear = releaseYear;
        InputValidator.validatePositiveDuration(durationMinutes);
        this.durationMinutes = durationMinutes;
        InputValidator.validateRating(rating);
        this.rating = rating;
        if (viewCount < 0) {
            throw new IllegalArgumentException("View count cannot be negative.");
        }
        this.viewCount = viewCount;
        this.isDeleted = isDeleted;
    }

    /**
     * Returns the unique movie identifier.
     *
     * @return the movie identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the movie title.
     *
     * @return the movie title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Updates the movie title.
     *
     * @param title the new movie title
     */
    public void setTitle(String title) {
        this.title = InputValidator.normalizeText(title, "Movie title");
    }

    /**
     * Returns the referenced category identifier.
     *
     * @return the category identifier
     */
    public String getCategoryId() {
        return this.categoryId;
    }

    /**
     * Updates the category association.
     *
     * @param categoryId the new category identifier
     */
    public void setCategoryId(String categoryId) {
        this.categoryId = InputValidator.normalizeId(categoryId, 'G');
    }

    /**
     * Returns the director name.
     *
     * @return the director name
     */
    public String getDirector() {
        return this.director;
    }

    /**
     * Updates the director name.
     *
     * @param director the new director name
     */
    public void setDirector(String director) {
        this.director = InputValidator.normalizeText(director, "Director");
    }

    /**
     * Returns the cast list.
     *
     * @return the cast list
     */
    public String getCast() {
        return this.cast;
    }

    /**
     * Updates the cast list.
     *
     * @param cast the new cast list
     */
    public void setCast(String cast) {
        this.cast = (cast == null) ? "" : cast.trim();
    }

    /**
     * Returns the release year.
     *
     * @return the release year
     */
    public int getReleaseYear() {
        return this.releaseYear;
    }

    /**
     * Updates the release year.
     *
     * @param releaseYear the new release year
     */
    public void setReleaseYear(int releaseYear) {
        InputValidator.validateReleaseYear(releaseYear);
        this.releaseYear = releaseYear;
    }

    /**
     * Returns total runtime in minutes.
     *
     * @return runtime in minutes
     */
    public int getDurationMinutes() {
        return this.durationMinutes;
    }

    /**
     * Updates runtime duration in minutes.
     *
     * @param durationMinutes runtime duration in minutes
     */
    public void setDurationMinutes(int durationMinutes) {
        InputValidator.validatePositiveDuration(durationMinutes);
        this.durationMinutes = durationMinutes;
    }

    /**
     * Returns the critic rating.
     *
     * @return the rating
     */
    public double getRating() {
        return this.rating;
    }

    /**
     * Updates the critic rating.
     *
     * @param rating the new rating
     */
    public void setRating(double rating) {
        InputValidator.validateRating(rating);
        this.rating = rating;
    }

    /**
     * Returns cumulative view count.
     *
     * @return total views
     */
    public int getViewCount() {
        return this.viewCount;
    }

    /**
     * Increments the cumulative view count by one.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Decrements the cumulative view count by one.
     */
    public void decrementViewCount() {
        if (this.viewCount > 0) {
            this.viewCount--;
        }
    }

    /**
     * Returns whether the movie is soft-deleted.
     *
     * @return true if soft-deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * Updates the soft-delete status.
     *
     * @param isDeleted true to soft-delete, false to activate
     */
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
