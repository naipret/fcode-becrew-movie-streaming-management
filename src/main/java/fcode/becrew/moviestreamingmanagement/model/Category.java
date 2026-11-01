package fcode.becrew.moviestreamingmanagement.model;

import fcode.becrew.moviestreamingmanagement.util.InputValidator;

/**
 * Domain entity representing a movie genre or content category.
 */
public class Category {

    private final String id;
    private String name;
    private String description;
    private boolean isDeleted;

    /**
     * Constructs a Category entity.
     *
     * @param id          the unique category identifier (e.g. G00001)
     * @param name        the category name
     * @param description the narrative description of the category
     * @param isDeleted   true if soft-deleted, false if active
     */
    public Category(String id, String name, String description, boolean isDeleted) {
        this.id = InputValidator.normalizeId(id, 'G');
        this.name = InputValidator.normalizeText(name, "Category name");
        this.description = (description == null) ? "" : description.trim();
        this.isDeleted = isDeleted;
    }

    /**
     * Returns the unique category identifier.
     *
     * @return the category identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the category name.
     *
     * @return the category name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Updates the category name.
     *
     * @param name the new category name
     */
    public void setName(String name) {
        this.name = InputValidator.normalizeText(name, "Category name");
    }

    /**
     * Returns the category description.
     *
     * @return the category description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Updates the category description.
     *
     * @param description the new category description
     */
    public void setDescription(String description) {
        this.description = (description == null) ? "" : description.trim();
    }

    /**
     * Returns whether the category is soft-deleted.
     *
     * @return true if soft-deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * Updates the soft-deletion status.
     *
     * @param isDeleted true to soft-delete, false to activate
     */
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
