package fcode.becrew.moviestreamingmanagement.view;

import fcode.becrew.moviestreamingmanagement.controller.StreamingSessionController;
import fcode.becrew.moviestreamingmanagement.controller.SubscriberController;
import fcode.becrew.moviestreamingmanagement.model.StreamingSession;
import fcode.becrew.moviestreamingmanagement.model.Subscriber;
import fcode.becrew.moviestreamingmanagement.util.ConsoleUtil;
import fcode.becrew.moviestreamingmanagement.util.InputValidator;
import fcode.becrew.moviestreamingmanagement.util.TableFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Interactive 80-column console menu for managing subscriber accounts:
 * registering subscribers, directory lookup with session summary inspection,
 * updating profile information or tier, and soft-deleting profiles.
 */
public class SubscriberMenu extends Menu {

    private static final String[] MENU_OPTIONS = {
            "Register New Subscriber",
            "View All Subscribers",
            "Update Subscriber",
            "Remove Subscriber"
    };

    private static final String TABLE_BORDER = "+--------+----------------------+------------+----------------------+----------+";
    private static final String TABLE_HEADER = "| ID     | Full Name            | Phone      | Email                | Tier     |";

    private final SubscriberController subscriberController;
    private final StreamingSessionController sessionController;

    /**
     * Constructs a SubscriberMenu.
     *
     * @param subscriberController collaborator for subscriber profile operations
     * @param sessionController    collaborator for subscriber session statistics
     */
    public SubscriberMenu(SubscriberController subscriberController,
            StreamingSessionController sessionController) {
        super("MOVIE STREAMING MANAGEMENT SYSTEM > [3] SUBSCRIBERS", MENU_OPTIONS);
        if (subscriberController == null || sessionController == null) {
            throw new IllegalArgumentException("Controllers cannot be null.");
        }
        this.subscriberController = subscriberController;
        this.sessionController = sessionController;
    }

    @Override
    protected void handleChoice(int choice) {
        switch (choice) {
            case 1:
                handleRegisterSubscriber();
                break;
            case 2:
                handleViewAllSubscribers();
                break;
            case 3:
                handleUpdateSubscriber();
                break;
            case 4:
                handleRemoveSubscriber();
                break;
            default:
                System.out.println("[ERROR] Unsupported menu choice: " + choice);
                break;
        }
    }

    private void handleRegisterSubscriber() {
        while (true) {
            String fullName = ConsoleUtil.readString("Enter full name (or 0 to return): ");
            if (fullName == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            String phone = ConsoleUtil.readString("Enter 10-digit phone number (or 0 to cancel): ");
            if (phone == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            String email = ConsoleUtil.readString("Enter email address (or 0 to cancel): ");
            if (email == null) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            System.out.println("Select Subscription Tier:");
            System.out.println("  [1] Standard ($7.99/mo, 2 streams)");
            System.out.println("  [2] Premium  ($14.99/mo, 4 streams)");
            int tierChoice = ConsoleUtil.readInt("Choice [1-2, 0 to cancel]: ", 0, 2);
            if (tierChoice == 0) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            String tier = (tierChoice == 1) ? "Standard" : "Premium";

            System.out.println();
            System.out.println(TABLE_BORDER);
            System.out.println(TABLE_HEADER);
            System.out.println(TABLE_BORDER);
            printSubscriberRow("PREVIEW", fullName, phone, email, tier);
            System.out.println(TABLE_BORDER);

            if (!ConsoleUtil.confirm("Confirm registering this subscriber?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                Subscriber sub = this.subscriberController.registerSubscriber(fullName, phone, email, tier);
                System.out.println("[OK] Subscriber " + sub.getId() + " registered successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleViewAllSubscribers() {
        while (true) {
            List<Subscriber> active = this.subscriberController.getAllActiveSubscribers();
            renderSubscriberTable(active);

            System.out.println("Options: [Subscriber ID to inspect profile] | [Keyword to search] | [0] Return");
            String input = ConsoleUtil.readOptionalString("Enter selection: ");
            if ("0".equals(input)) {
                return;
            }
            if (input.isEmpty()) {
                continue;
            }

            Subscriber selectedSub = this.subscriberController.findById(input);
            if (selectedSub != null) {
                renderSubscriberDetailCard(selectedSub);
                continue;
            }

            List<Subscriber> searchResults = this.subscriberController.search(input);
            if (searchResults.isEmpty()) {
                System.out.println("[INFO] No active subscribers found matching '" + input + "'.");
            } else {
                System.out.println();
                System.out.println("  Search Results for: '" + input + "'");
                renderSubscriberTable(searchResults);
            }
        }
    }

    private void renderSubscriberDetailCard(Subscriber sub) {
        List<StreamingSession> sessions = this.sessionController.getSessionsBySubscriber(sub.getId());
        long totalMinutes = 0;
        int completed = 0;
        for (int i = 0; i < sessions.size(); i++) {
            totalMinutes += sessions.get(i).getWatchedMinutes();
            if (sessions.get(i).isCompleted()) {
                completed++;
            }
        }
        double totalHours = totalMinutes / 60.0;
        double completionRate = (sessions.isEmpty()) ? 0.0 : (completed * 100.0) / sessions.size();

        String border = "+" + TableFormatter.repeat('-', TableFormatter.SCREEN_WIDTH - 2) + "+";
        System.out.println();
        System.out.println(border);
        System.out.println("| " + TableFormatter.fit("SUBSCRIBER PROFILE: " + sub.getId() + " - " + sub.getFullName(),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);
        System.out.println("| "
                + TableFormatter.fit("Phone Number   : " + sub.getPhone(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| "
                + TableFormatter.fit("Email Address  : " + sub.getEmail(), TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| "
                + TableFormatter.fit("Plan Tier      : " + sub.getTierName() + " Tier", TableFormatter.SCREEN_WIDTH - 4)
                + " |");
        System.out.println("| " + TableFormatter.fit(
                "Monthly Fee    : " + String.format(Locale.US, "$%.2f / month", sub.calculateMonthlyFee()),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(
                "| " + TableFormatter.fit("Max Streams    : " + sub.getMaxConcurrentStreams() + " concurrent devices",
                        TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);
        System.out.println(
                "| " + TableFormatter.fit("STREAMING ACTIVITY SUMMARY", TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit("Total Sessions : " + sessions.size() + " recorded sessions",
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit(
                "Total Watch Time: " + String.format(Locale.US, "%.1f hours (%d minutes)", totalHours, totalMinutes),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println("| " + TableFormatter.fit(
                "Completion Rate: "
                        + String.format(Locale.US, "%.1f%% (%d completed movies)", completionRate, completed),
                TableFormatter.SCREEN_WIDTH - 4) + " |");
        System.out.println(border);
        System.out.println();
    }

    private void handleUpdateSubscriber() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Subscriber ID to update (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Subscriber sub = this.subscriberController.findById(rawId);
            if (sub == null) {
                System.out.println("[ERROR] Active subscriber not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleSubscriberTable(sub);

            System.out.println("Press [Enter] to keep current value, or type 0 to cancel:");
            String newName = ConsoleUtil.readOptionalString("Full Name [" + sub.getFullName() + "]: ");
            if ("0".equals(newName)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newName.isEmpty()) {
                newName = sub.getFullName();
            }

            String newPhone = ConsoleUtil.readOptionalString("Phone [" + sub.getPhone() + "]: ");
            if ("0".equals(newPhone)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newPhone.isEmpty()) {
                newPhone = sub.getPhone();
            }

            String newEmail = ConsoleUtil.readOptionalString("Email [" + sub.getEmail() + "]: ");
            if ("0".equals(newEmail)) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            if (newEmail.isEmpty()) {
                newEmail = sub.getEmail();
            }

            System.out.println("Current Tier: " + sub.getTierName());
            System.out.println("  [1] Keep Current Tier (" + sub.getTierName() + ")");
            System.out.println("  [2] Switch to Standard ($7.99/mo)");
            System.out.println("  [3] Switch to Premium  ($14.99/mo)");
            int tierChoice = ConsoleUtil.readInt("Select tier [1-3, 0 to cancel]: ", 0, 3);
            if (tierChoice == 0) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }
            String newTier = sub.getTierName();
            if (tierChoice == 2) {
                newTier = "Standard";
            } else if (tierChoice == 3) {
                newTier = "Premium";
            }

            if (!ConsoleUtil.confirm("Confirm updating subscriber " + sub.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.subscriberController.updateSubscriber(sub.getId(), newName, newPhone, newEmail, newTier);
                System.out.println("[OK] Subscriber " + sub.getId() + " updated successfully.");
                Subscriber updated = this.subscriberController.findById(sub.getId());
                if (updated != null) {
                    renderSingleSubscriberTable(updated);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void handleRemoveSubscriber() {
        while (true) {
            String rawId = ConsoleUtil.readString("Enter Subscriber ID to remove (or 0 to return): ");
            if (rawId == null) {
                System.out.println("[INFO] Operation cancelled. Returning to menu.");
                return;
            }

            Subscriber sub = this.subscriberController.findById(rawId);
            if (sub == null) {
                System.out.println("[ERROR] Active subscriber not found with ID: " + rawId);
                printDivider();
                continue;
            }

            System.out.println();
            renderSingleSubscriberTable(sub);

            if (!ConsoleUtil.confirm("Are you sure you want to remove subscriber " + sub.getId() + "?")) {
                System.out.println("[INFO] Operation cancelled.");
                printDivider();
                continue;
            }

            try {
                this.subscriberController.deleteSubscriber(sub.getId());
                System.out.println("[OK] Subscriber " + sub.getId() + " soft-deleted successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            printDivider();
        }
    }

    private void renderSubscriberTable(List<Subscriber> subscriberList) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        if (subscriberList.isEmpty()) {
            System.out.println(
                    "|" + TableFormatter.center("No active subscribers found.", TableFormatter.SCREEN_WIDTH - 2) + "|");
        } else {
            for (int i = 0; i < subscriberList.size(); i++) {
                Subscriber s = subscriberList.get(i);
                printSubscriberRow(s.getId(), s.getFullName(), s.getPhone(), s.getEmail(), s.getTierName());
            }
        }
        System.out.println(TABLE_BORDER);
    }

    private void renderSingleSubscriberTable(Subscriber s) {
        System.out.println(TABLE_BORDER);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_BORDER);
        printSubscriberRow(s.getId(), s.getFullName(), s.getPhone(), s.getEmail(), s.getTierName());
        System.out.println(TABLE_BORDER);
    }

    private void printSubscriberRow(String id, String name, String phone, String email, String tier) {
        System.out.printf("| %-6s | %-20s | %-10s | %-20s | %-8s |%n",
                TableFormatter.fit(id, 6),
                TableFormatter.fit(name, 20),
                TableFormatter.fit(phone, 10),
                TableFormatter.fit(email, 20),
                TableFormatter.fit(tier, 8));
    }
}
