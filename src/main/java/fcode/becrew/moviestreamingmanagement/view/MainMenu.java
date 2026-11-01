package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;

/**
 * Root interactive menu for the Movie Streaming Management System,
 * presenting single-word primary domain choices and dispatching to
 * dedicated functional submenus.
 */
public class MainMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Movies",
            "Categories",
            "Subscribers",
            "Sessions",
            "Reports"
    };

    private final MovieMenu movieMenu;
    private final CategoryMenu categoryMenu;
    private final SubscriberMenu subscriberMenu;
    private final StreamingSessionMenu streamingSessionMenu;
    private final AnalyticsMenu analyticsMenu;

    /**
     * Constructs the root MainMenu wiring all functional submenus.
     *
     * @param movieMenu            the Movies management submenu
     * @param categoryMenu         the Categories management submenu
     * @param subscriberMenu       the Subscribers management submenu
     * @param streamingSessionMenu the Streaming Sessions submenu
     * @param analyticsMenu        the Analytics and Reports submenu
     * @throws IllegalArgumentException if any submenu is null
     */
    public MainMenu(MovieMenu movieMenu,
                    CategoryMenu categoryMenu,
                    SubscriberMenu subscriberMenu,
                    StreamingSessionMenu streamingSessionMenu,
                    AnalyticsMenu analyticsMenu) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM", MENU_OPTIONS);
        if (movieMenu == null || categoryMenu == null || subscriberMenu == null
                || streamingSessionMenu == null || analyticsMenu == null) {
            throw new IllegalArgumentException("Submenus cannot be null.");
        }
        this.movieMenu = movieMenu;
        this.categoryMenu = categoryMenu;
        this.subscriberMenu = subscriberMenu;
        this.streamingSessionMenu = streamingSessionMenu;
        this.analyticsMenu = analyticsMenu;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                this.movieMenu.display();
                break;
            case 2:
                this.categoryMenu.display();
                break;
            case 3:
                this.subscriberMenu.display();
                break;
            case 4:
                this.streamingSessionMenu.display();
                break;
            case 5:
                this.analyticsMenu.display();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    @Override
    protected boolean shouldShowBreadcrumb() {
        return false;
    }

    @Override
    protected String getZeroOptionLabel() {
        return "Exit";
    }

    @Override
    protected void onExit() {
        ConsoleUtil.clearScreen();
    }
}
