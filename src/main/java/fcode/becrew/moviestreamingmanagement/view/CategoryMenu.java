package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.controller.CategoryController;
import fcode.becrew.moviestreamingmanagement.controller.MovieController;
import fcode.becrew.moviestreamingmanagement.model.Category;
import fcode.becrew.moviestreamingmanagement.model.Movie;
import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;
import fcode.becrew.moviestreamingmanagement.util.TableFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interactive 80-column console menu for managing content categories and
 * genres:
 * adding categories, viewing directory with movie counts, browsing movies by
 * category,
 * updating attributes, and soft-deleting with foreign key integrity protection.
 */
public class CategoryMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Add New Category",
            "View All Categories",
            "Update Category",
            "Remove Category"
    };

    private static final String TABLE_BORDER = "+--------+--------------------+--------------------------------------+--------+";
    private static final String TABLE_HEADER = "| ID     | Category Name      | Description                          | Movies |";

    private final CategoryController categoryController;
    private final MovieController movieController;

    /**
     * Constructs a CategoryMenu.
     *
     * @param categoryController collaborator for category operations
     * @param movieController    collaborator for verifying movie foreign key
     *                           constraints
     */
    public CategoryMenu(CategoryController categoryController, MovieController movieController) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM > [2] CATEGORIES", MENU_OPTIONS);
        if (categoryController == null || movieController == null) {
            throw new IllegalArgumentException("Controllers cannot be null.");
        }
        this.categoryController = categoryController;
        this.movieController = movieController;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                handleAddCategory();
                break;
            case 2:
                handleViewAllCategories();
                break;
            case 3:
                handleUpdateCategory();
                break;
            case 4:
                handleRemoveCategory();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    private void handleAddCategory() {
        while (true) {
            String name = ConsoleUtil.readString("Enter category name (or 0 to return): ");
            if (name == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            String desc = ConsoleUtil.readOptionalString("Enter description (or Enter to skip): ");
            if ("0".equals(desc)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            System.out.println();
            System.out.println(TABLE_BORDER);
            System.out.println(TABLE_HEADER);
            System.out.println(TABLE_BORDER);
            printCategoryRow("PREVIEW", name, desc, 0);
            System.out.println(TABLE_BORDER);

            if (!ConsoleUtil.confirm("Confirm adding this category?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                Category cat = this.categoryController.addCategory(name, desc);
                System.out.println("[OK] Category " + cat.getId() + " added successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleViewAllCategories() {
        while (true) {
            List<Category> active = this.categoryController.getAllActiveCategories();
            renderCategoryTable(active);

            System.out.println("Options: [Category ID to browse its movies] | [Keyword to search] | [0] Return");
            String input = ConsoleUtil.readOptionalString("Enter selection: ");
            if ("0".equals(input)) {
                return;
            }
            if (input.isEmpty()) {
                continue;
            }

            Category selectedCategory = this.categoryController.findById(input);
            if (selectedCategory != null) {
                browseMoviesByCategory(selectedCategory);
                continue;
            }

            List<Category> matching = new ArrayList<>();
            String query = input.toLowerCase(Locale.US);
            for (int i = 0; i < active.size(); i++) {
                Category c = active.get(i);
                if (c.getId().toLowerCase(Locale.US).contains(query)
                        || c.getName().toLowerCase(Locale.US).contains(query)
                        || c.getDescription().toLowerCase(Locale.US).contains(query)) {
                    matching.add(c);
                }
            }

            if (matching.isEmpty()) {
                System.out.println("[INFO] No active categories found matching '" + input + "'.");
            } else {
                System.out.println();
                System.out.println("  Search Results for: '" + input + "'");
                renderCategoryTable(matching);
            }
        }
    }

    private void browseMoviesByCategory(Category category) {
        String border = "+--------+--------------------------+------+---------+--------+------------+";
        String header = "| ID     | Title                    | Year | Dur     | Rating | Views      |";

        System.out.println();
        System.out.println("  MOVIES IN CATEGORY: " + category.getId() + " - " + category.getName());
        System.out.println(border);
        System.out.println(header);
        System.out.println(border);

        List<Movie> allMovies = this.movieController.getAllActiveMovies();
        int count = 0;
        for (int i = 0; i < allMovies.size(); i++) {
            Movie m = allMovies.get(i);
            if (m.getCategoryId().equalsIgnoreCase(category.getId())) {
                System.out.printf("| %-6s | %-24s | %4d | %4d min | %6.1f | %10s |%n",
                        TableFormatter.fit(m.getId(), 6),
                        TableFormatter.fit(m.getTitle(), 24),
                        m.getReleaseYear(),
                        m.getDurationMinutes(),
                        m.getRating(),
                        TableFormatter.fitRight(String.format(Locale.US, "%,d", m.getViewCount()), 10));
                count++;
            }
        }

        if (count == 0) {
            System.out.println(
                    "|" + TableFormatter.center("No active movies in this category.", TableFormatter.SCREEN_WIDTH - 2)
                            + "|");
        }
        System.out.println(border);
        System.out.println();
    }

    private void handleUpdateCategory() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Category ID to update (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Category cat = this.categoryController.findById(rawId);
            if (cat == null) {
                System.out.println("[ERROR] Active category not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleCategoryTable(cat);

            System.out.println("Press [Enter] to keep current value, or type 0 to cancel:");
            String newName = ConsoleUtil.readOptionalString("Category Name [" + cat.getName() + "]: ");
            if ("0".equals(newName)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newName.isEmpty()) {
                newName = cat.getName();
            }

            String newDesc = ConsoleUtil.readOptionalString("Description [" + cat.getDescription() + "]: ");
            if ("0".equals(newDesc)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newDesc.isEmpty()) {
                newDesc = cat.getDescription();
            }

            if (!ConsoleUtil.confirm("Confirm updating category " + cat.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.categoryController.updateCategory(cat.getId(), newName, newDesc);
                System.out.println("[OK] Category " + cat.getId() + " updated successfully.");
                renderSingleCategoryTable(cat);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleRemoveCategory() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Category ID to remove (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Category cat = this.categoryController.findById(rawId);
            if (cat == null) {
                System.out.println("[ERROR] Active category not found with ID: " + rawId);
                printDivider();
                continue;
            }

            boolean hasMovies = this.movieController.hasActiveMoviesForCategory(cat.getId());
            if (hasMovies) {
                System.out.println("[WARN] Cannot delete category '" + cat.getName()
                        + "' because active movies currently reference it.");
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleCategoryTable(cat);

            if (!ConsoleUtil.confirm("Are you sure you want to remove category " + cat.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.categoryController.deleteCategory(cat.getId(), false);
                System.out.println("[OK] Category " + cat.getId() + " soft-deleted successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void renderCategoryTable(List<Category> categoryList) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        if (categoryList.isEmpty()) {
            System.out.println(
                    "|" + TableFormatter.center("No active categories found.", TableFormatter.SCREEN_WIDTH - 2) + "|");
        } else {
            for (int i = 0; i < categoryList.size(); i++) {
                Category c = categoryList.get(i);
                int movieCount = countMoviesForCategory(c.getId());
                printCategoryRow(c.getId(), c.getName(), c.getDescription(), movieCount);
            }
        }
        System.out.println(TABLE_BORDER);
    }

    private void renderSingleCategoryTable(Category cat) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        int movieCount = countMoviesForCategory(cat.getId());
        printCategoryRow(cat.getId(), cat.getName(), cat.getDescription(), movieCount);
        System.out.println(TABLE_BORDER);
    }

    private void printCategoryRow(String id, String name, String desc, int count) {
        System.out.printf("| %-6s | %-18s | %-36s | %6d |%n",
                TableFormatter.fit(id, 6),
                TableFormatter.fit(name, 18),
                TableFormatter.fit(desc, 36),
                count);
    }

    private int countMoviesForCategory(String categoryId) {
        List<Movie> all = this.movieController.getAllActiveMovies();
        int count = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getCategoryId().equalsIgnoreCase(categoryId)) {
                count++;
            }
        }
        return count;
    }
}
