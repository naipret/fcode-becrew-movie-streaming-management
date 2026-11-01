# Console User Interface Specification

This document defines the terminal layout constraints, navigation conventions, input parsing rules, and 80-column ASCII table designs for all console views in `fcode.becrew.moviestreamingmanagement.view`.

---

## 1. Terminal Standards and Formatting Rules

| Attribute | Standard |
| :--- | :--- |
| Maximum Line Width | Strictly `80` characters per line (`TableFormatter.SCREEN_WIDTH = 80`) |
| Border Character Set | Basic 7-bit ASCII characters only (`+`, `-`, `=`, `\|`, `*`). Box-drawing Unicode and ANSI escape colors are prohibited to ensure clean rendering across all terminal emulators and IDE consoles. |
| Interface Language | English across all menu titles, prompts, table headers, and status messages |
| Character Encoding | Pure 100% 7-bit ASCII text across all prompts, tables, and seeded CSV records |
| Date Format | `dd/MM/yyyy` for console date prompts and table columns; timestamps use `dd/MM/yyyy HH:mm` |
| Number and Metric Format | Comma-separated integers for views (`45,200`), minutes with suffix (`148 min`), ratings to 1 decimal place (`8.8`), and currency in US format (`$14.99`) |

---

## 2. Navigation and Input Conventions

### 2.1. Menu Selection and Universal Cancellation

Every menu rendered via `Menu.display()` lists numbered options starting from `[1]` through `[N]` and places `[0]` at the bottom to exit or return:

1. In `MainMenu`, selecting `[0]` exits the application cleanly.
2. In submenus (`MovieMenu`, `CategoryMenu`, `SubscriberMenu`, `StreamingSessionMenu`, `AnalyticsMenu`), selecting `[0]` returns immediately to `MainMenu`.
3. Across all input fields in all menus, entering `0` immediately cancels the ongoing operation and safely returns to the menu without modifying persistent storage.

### 2.2. Default Values and Enter-to-Keep Convention

1. When updating existing records, pressing `[Enter]` without typing any input retains the current value for that field. Current values are displayed before prompting, allowing the operator to press `Enter to keep, or 0 to cancel`.
2. Report Date and Window Defaults:
   - In Trending Categories, pressing `[Enter]` defaults to a `7` days window.
   - In Top-K Movie Ranking, pressing `[Enter]` defaults to limit `10`.
   - In Monthly Performance Report, pressing `[Enter]` defaults to the current month in `MM/yyyy` format.

### 2.3. System-Managed Identifiers

All entity identifiers (`M00001` for movies, `G00001` for categories, `U00001` for subscribers, `S00001` for streaming sessions) are generated automatically and sequentially by controllers. Operators never invent or type new IDs during creation.

### 2.4. Dual Table Verification and Confirmation

1. In all update and removal workflows, the system displays the current entity state in a formatted 80-column ASCII table before modifications, and displays the refreshed table immediately after changes are saved.
2. All create, update, and remove actions require an explicit `[Y/N]` confirmation step (`Are you sure ...? [Y/N]:`) before persisting changes to disk.

### 2.5. Standardized Continuous Loop UX

All feature screens loop continuously in a standardized execution model:

1. After completing or cancelling an operation, the feature remains active for the next item without forcing the operator to re-select the menu option.
2. Each completed or cancelled operation iteration is cleanly demarcated by an empty line followed by an 80-character horizontal divider (`--------------------------------------------------------------------------------`) via `Menu.printDivider()`.
3. Entering `0` at the primary prompt immediately breaks out of the loop and returns to the menu frame. Entering `0` or `N` at a secondary prompt cancels that single transaction, prints the iteration divider, and continues the loop.

### 2.6. Screen Clearing and Breadcrumb Navigation

1. Before rendering any menu frame and after completing major actions, the terminal viewport is cleared using ANSI escape sequence `\033[H\033[2J` preceded by a newline.
2. Every dispatched submenu action renders an 80-column centered breadcrumb banner at the top of the screen before executing the selected handler (for example, `MAIN MENU > [1] MOVIES > [1] Add New Movie`).

### 2.7. Standardized Status Prefixes

All console feedback messages use explicit ASCII status prefixes:

| Prefix | Meaning | Example |
| :--- | :--- | :--- |
| `[OK]` | Operation completed and persisted successfully | `[OK] Movie M00001 added successfully.` |
| `[ERROR]` | Validation failure or business invariant violation | `[ERROR] Duration must be greater than zero.` |
| `[WARN]` | Non-fatal warning or condition alert | `[WARN] Cannot delete category G00002 because active movies reference it.` |
| `[INFO]` | Informational message or empty query notice | `[INFO] Operation cancelled. Returning to menu.` |

---

## 3. String Truncation and Alignment Rules

To guarantee that long movie titles, director names, or emails never wrap past column 80 and break table borders, all cell values pass through `TableFormatter`:

| Method | Behavior |
| :--- | :--- |
| `TableFormatter.fit(String text, int width)` | If `text.length() <= width`, right-pads with spaces to exactly `width`. If `text.length() > width` and `width > 2`, truncates the text to `width - 2` characters and appends `..`. |
| `TableFormatter.center(String text, int width)` | Centers `text` within `width` characters using balanced space padding, truncating via `fit` if needed. |
| `TableFormatter.repeat(char ch, int count)` | Generates a horizontal border line of `count` repeated ASCII characters (`=` or `-`). |

---

## 4. 80-Column Table Layouts

### 4.1. Main Menu Frame

The root main menu strictly uses single-word labels for all primary domain menus:

```text
================================================================================
                       MOVIE STREAMING MANAGEMENT SYSTEM
================================================================================
  [1] Movies
  [2] Categories
  [3] Subscribers
  [4] Sessions
  [5] Reports
  [0] Exit
--------------------------------------------------------------------------------
Enter your choice [0-5]:
```

### 4.2. Movie Catalog Table

Formula: `1 + (8) + 1 + (26) + 1 + (14) + 1 + (6) + 1 + (9) + 1 + (8) + 1 = 80` columns.

```text
+--------+--------------------------+--------------+------+---------+--------+
| ID     | Title                    | Category     | Year | Dur     | Rating |
+--------+--------------------------+--------------+------+---------+--------+
| M00001 | Inception                | Sci-Fi       | 2010 | 148 min |    8.8 |
| M00002 | Interstellar             | Sci-Fi       | 2014 | 169 min |    8.7 |
| M00003 | The Dark Knight          | Action       | 2008 | 152 min |    9.0 |
+--------+--------------------------+--------------+------+---------+--------+
```

### 4.3. Dedicated Movie Detail Inspection Card

```text
+------------------------------------------------------------------------------+
| MOVIE DETAILS: M00001 - Inception                                            |
+------------------------------------------------------------------------------+
| Category   : Sci-Fi (G00001)                                                 |
| Director   : Christopher Nolan                                               |
| Cast       : Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page            |
| Released   : 2010                                                            |
| Duration   : 148 minutes                                                     |
| Rating     : 8.8 / 10.0                                                      |
| Total Views: 45,200 views                                                    |
| Status     : Active                                                          |
+------------------------------------------------------------------------------+
| SYNOPSIS:                                                                    |
| A thief who steals corporate secrets through dream-sharing technology is     |
| given the inverse task of planting an idea into the mind of a C.E.O.         |
+------------------------------------------------------------------------------+
```

### 4.4. Category Catalog Table

Formula: `1 + (8) + 1 + (20) + 1 + (38) + 1 + (8) + 1 = 80` columns.

```text
+--------+--------------------+--------------------------------------+--------+
| ID     | Category Name      | Description                          | Movies |
+--------+--------------------+--------------------------------------+--------+
| G00001 | Sci-Fi             | Science fiction and speculative tech |     12 |
| G00002 | Action             | High intensity stunts and spectacles |     18 |
| G00003 | Drama              | Character-driven narrative stories   |     15 |
+--------+--------------------+--------------------------------------+--------+
```

### 4.5. Subscriber Directory Table

Formula: `1 + (8) + 1 + (22) + 1 + (12) + 1 + (22) + 1 + (10) + 1 = 80` columns.

```text
+--------+----------------------+------------+----------------------+----------+
| ID     | Full Name            | Phone      | Email                | Tier     |
+--------+----------------------+------------+----------------------+----------+
| U00001 | Nguyen Van An        | 0901234567 | an.nguyen@domain.com | Premium  |
| U00002 | Tran Thi Binh        | 0912345678 | binh.tran@domain.com | Standard |
+--------+----------------------+------------+----------------------+----------+
```

### 4.6. Streaming Sessions Log Table

Formula: `1 + (8) + 1 + (8) + 1 + (8) + 1 + (18) + 1 + (10) + 1 + (10) + 1 + (10) + 1 = 80` columns.

```text
+--------+--------+--------+------------------+----------+----------+----------+
| SessID | SubID  | MovID  | Session Date     | Watched  | Duration | Status   |
+--------+--------+--------+------------------+----------+----------+----------+
| S00001 | U00001 | M00001 | 28/10/2026 20:15 |  148 min |  148 min | Complete |
| S00002 | U00002 | M00002 | 30/10/2026 21:00 |   45 min |  169 min | Partial  |
+--------+--------+--------+------------------+----------+----------+----------+
```

### 4.7. Top-K Ranked Movies Table

Formula: `1 + (6) + 1 + (8) + 1 + (26) + 1 + (8) + 1 + (12) + 1 + (12) + 1 = 80` columns.

```text
+------+--------+--------------------------+--------+------------+------------+
| Rank | ID     | Title                    | Rating | Views      | Score      |
+------+--------+--------------------------+--------+------------+------------+
|    1 | M00003 | The Dark Knight          |    9.0 |     58,400 |      9.421 |
|    2 | M00001 | Inception                |    8.8 |     45,200 |      9.115 |
|    3 | M00002 | Interstellar             |    8.7 |     52,100 |      9.084 |
+------+--------+--------------------------+--------+------------+------------+
```

### 4.8. Trending Categories Table

Formula: `1 + (6) + 1 + (8) + 1 + (24) + 1 + (16) + 1 + (18) + 1 = 80` columns.

```text
+------+--------+--------------------------+------------------+------------------+
| Rank | CatID  | Category Name            | Total Hours      | Total Sessions   |
+------+--------+--------------------------+------------------+------------------+
|    1 | G00001 | Sci-Fi                   |        284.5 hrs |              118 |
|    2 | G00002 | Action                   |        192.0 hrs |               82 |
|    3 | G00003 | Drama                    |        145.2 hrs |               64 |
+------+--------+--------------------------+------------------+------------------+
```

### 4.9. Monthly Streaming Performance Report

```text
================================================================================
             MONTHLY STREAMING PERFORMANCE REPORT - OCTOBER 2026
================================================================================
  Total Active Subscribers          : 42 subscribers
  Total Streaming Sessions Recorded : 384 sessions
  Total Streaming Time              : 742.5 hours
  Overall Completion Rate           : 76.4 %
  Estimated Monthly Subscription Rev: $ 482.58
--------------------------------------------------------------------------------
```

---

## 5. Submenu Hierarchies and Options

All submenus strictly enforce CRUD ordering followed by analytical queries:

1. Movies Menu (`MovieMenu`):
   - `[1] Add New Movie`
   - `[2] View All Movies` (interactive table loop with keyword search, `F` to filter, `S` to sort, `R` to reset, and ID inspection)
   - `[3] Update Movie`
   - `[4] Remove Movie`
   - `[0] Back to Main Menu`

2. Categories Menu (`CategoryMenu`):
   - `[1] Add New Category`
   - `[2] View All Categories` (table listing; entering Category ID inspects and displays all movies in that category)
   - `[3] Update Category`
   - `[4] Remove Category`
   - `[0] Back to Main Menu`

3. Subscribers Menu (`SubscriberMenu`):
   - `[1] Register New Subscriber`
   - `[2] View All Subscribers` (directory table with keyword search and subscriber session summary inspection)
   - `[3] Update Subscriber`
   - `[4] Remove Subscriber`
   - `[0] Back to Main Menu`

4. Streaming Sessions Menu (`StreamingSessionMenu`):
   - `[1] Record Streaming Session`
   - `[2] View All Streaming Sessions` (session history table with subscriber and movie filtering)
   - `[3] Cancel Streaming Session`
   - `[0] Back to Main Menu`

5. Reports Menu (`AnalyticsMenu`):
   - `[1] Platform Viewing Statistics`
   - `[2] Trending Categories Report` (prompts window days, Enter defaults to 7)
   - `[3] Top-K Ranked Movies` (prompts limit, Enter defaults to 10)
   - `[4] Monthly Performance Report` (prompts `MM/yyyy`, Enter defaults to current month)
   - `[0] Back to Main Menu`
