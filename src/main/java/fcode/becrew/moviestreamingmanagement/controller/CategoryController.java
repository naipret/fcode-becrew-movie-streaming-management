package fcode.becrew.moviestreamingmanagement.controller;

import fcode.becrew.moviestreamingmanagement.model.Category;
import fcode.becrew.moviestreamingmanagement.util.CsvUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Controller managing category registry state, case-insensitive uniqueness
 * invariants,
 * foreign key deletion constraints, and atomic CSV persistence.
 */
public class CategoryController {

    private static final String CSV_HEADER = "id,name,description,isDeleted";

    private final Path csvPath;
    private final List<Category> categories;

    /**
     * Constructs a CategoryController and loads persistent categories from the
     * given CSV path.
     *
     * @param csvPath the filesystem Path to data/categories.csv
     */
    public CategoryController(Path csvPath) {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path cannot be null.");
        }
        this.csvPath = csvPath;
        this.categories = new ArrayList<>();
        loadFromCsv();
    }

    /**
     * Loads category records from the configured CSV file into memory.
     */
    public final void loadFromCsv() {
        this.categories.clear();
        List<String[]> rows = CsvUtil.readCsv(this.csvPath);
        if (rows.isEmpty()) {
            return;
        }

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 4) {
                continue;
            }
            try {
                String id = row[0].trim();
                String name = row[1].trim();
                String description = row[2].trim();
                boolean isDeleted = Boolean.parseBoolean(row[3].trim());
                this.categories.add(new Category(id, name, description, isDeleted));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Persists all in-memory category records atomically to the CSV file.
     */
    public void saveToCsv() {
        List<String[]> rows = new ArrayList<>(this.categories.size() + 1);
        rows.add(CSV_HEADER.split(","));
        for (int i = 0; i < this.categories.size(); i++) {
            Category cat = this.categories.get(i);
            rows.add(new String[] {
                    cat.getId(),
                    cat.getName(),
                    cat.getDescription(),
                    Boolean.toString(cat.isDeleted())
            });
        }
        CsvUtil.writeToFile(this.csvPath, rows);
    }

    /**
     * Adds a new category after validating name uniqueness.
     *
     * @param name        the category name
     * @param description the narrative description
     * @return the created Category entity
     * @throws IllegalArgumentException if name is duplicate or invalid
     */
    public Category addCategory(String name, String description) {
        String cleanName = InputValidator.normalizeText(name, "Category name");
        Category existing = findByName(cleanName);
        if (existing != null) {
            throw new IllegalArgumentException("Category with name '" + cleanName + "' already exists.");
        }

        String nextId = generateNextId();
        Category category = new Category(nextId, cleanName, description, false);
        this.categories.add(category);
        saveToCsv();
        return category;
    }

    /**
     * Updates an existing category name and description.
     *
     * @param id          the category identifier
     * @param name        the updated category name
     * @param description the updated description
     * @throws IllegalArgumentException if category does not exist or new name
     *                                  conflicts
     */
    public void updateCategory(String id, String name, String description) {
        Category category = findById(id);
        if (category == null) {
            throw new IllegalArgumentException("Category not found with ID: " + id);
        }
        String cleanName = InputValidator.normalizeText(name, "Category name");
        if (!category.getName().equalsIgnoreCase(cleanName)) {
            Category existing = findByName(cleanName);
            if (existing != null && !existing.getId().equals(category.getId())) {
                throw new IllegalArgumentException("Category with name '" + cleanName + "' already exists.");
            }
        }
        category.setName(cleanName);
        category.setDescription(description);
        saveToCsv();
    }

    /**
     * Soft-deletes a category, verifying that no active movies reference it.
     *
     * @param id              the category identifier to delete
     * @param hasActiveMovies true if active movies still reference this category
     * @throws IllegalArgumentException if category does not exist or has
     *                                  referencing movies
     */
    public void deleteCategory(String id, boolean hasActiveMovies) {
        Category category = findById(id);
        if (category == null) {
            throw new IllegalArgumentException("Category not found with ID: " + id);
        }
        if (hasActiveMovies) {
            throw new IllegalArgumentException("Cannot delete category '" + category.getName()
                    + "' because active movies currently reference it.");
        }
        category.setDeleted(true);
        saveToCsv();
    }

    /**
     * Finds an active category by its identifier.
     *
     * @param id the category identifier, may be shorthand
     * @return the active Category, or null if not found or deleted
     */
    public Category findById(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'G');
            for (int i = 0; i < this.categories.size(); i++) {
                Category cat = this.categories.get(i);
                if (cat.getId().equalsIgnoreCase(canonicalId) && !cat.isDeleted()) {
                    return cat;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds a category by identifier regardless of soft-delete status.
     *
     * @param id the category identifier
     * @return the Category entity, or null if nonexistent
     */
    public Category findByIdIncludeDeleted(String id) {
        try {
            String canonicalId = InputValidator.normalizeId(id, 'G');
            for (int i = 0; i < this.categories.size(); i++) {
                Category cat = this.categories.get(i);
                if (cat.getId().equalsIgnoreCase(canonicalId)) {
                    return cat;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Finds an active category by case-insensitive name match.
     *
     * @param name the category name to search
     * @return matching active Category, or null if none found
     */
    public Category findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String clean = name.trim();
        for (int i = 0; i < this.categories.size(); i++) {
            Category cat = this.categories.get(i);
            if (!cat.isDeleted() && cat.getName().equalsIgnoreCase(clean)) {
                return cat;
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable list of all active categories.
     *
     * @return unmodifiable list of active Category entities
     */
    public List<Category> getAllActiveCategories() {
        List<Category> active = new ArrayList<>();
        for (int i = 0; i < this.categories.size(); i++) {
            Category cat = this.categories.get(i);
            if (!cat.isDeleted()) {
                active.add(cat);
            }
        }
        return Collections.unmodifiableList(active);
    }

    /**
     * Returns an unmodifiable list of all categories including soft-deleted ones.
     *
     * @return unmodifiable list of all Category records
     */
    public List<Category> getAllCategories() {
        return Collections.unmodifiableList(new ArrayList<>(this.categories));
    }

    private String generateNextId() {
        int max = 0;
        for (int i = 0; i < this.categories.size(); i++) {
            String rawId = this.categories.get(i).getId();
            if (rawId.length() >= 2 && rawId.charAt(0) == 'G') {
                try {
                    int num = Integer.parseInt(rawId.substring(1));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format(Locale.US, "G%05d", max + 1);
    }
}
