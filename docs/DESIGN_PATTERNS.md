# Design Patterns Specification

This document describes the five software design patterns implemented across the Movie Streaming Management System, detailing the specific problem each pattern addresses, its concrete implementation in the codebase, and its engineering trade-offs.

---

## 1. Factory Method Pattern

### 1.1. Context and Problem

When loading subscriber records from `data/subscribers.csv` or registering a new subscriber via `SubscriberController`, the system reads a string discriminator (`"Standard"` or `"Premium"`) and must instantiate the corresponding concrete subclass (`StandardSubscriber` or `PremiumSubscriber`). Scattering `if-else` or `switch` instantiation logic across controllers and menus tightly couples callers to every concrete subclass.

### 1.2. Implementation in Codebase

The abstract base class `Subscriber` provides a centralized static factory method:

```java
public static Subscriber create(String tier, String id, String fullName,
        String phone, String email, boolean isDeleted) {
    if (tier == null || tier.trim().isEmpty()) {
        throw new IllegalArgumentException("Subscriber tier cannot be empty.");
    }
    String normalizedTier = tier.trim();
    if ("Standard".equalsIgnoreCase(normalizedTier)) {
        return new StandardSubscriber(id, fullName, phone, email, isDeleted);
    }
    if ("Premium".equalsIgnoreCase(normalizedTier)) {
        return new PremiumSubscriber(id, fullName, phone, email, isDeleted);
    }
    throw new IllegalArgumentException("Unsupported subscriber tier: " + tier);
}
```

Placing the factory method directly on `Subscriber` follows the standard Java idiom (such as `LocalDate.of` or `Collections.unmodifiableList`), centralizing subclass creation without requiring an auxiliary `SubscriberFactory` class.

---

## 2. Atomic Persistence Pattern

### 2.1. Context and Problem

Writing directly to active persistent data files (`data/movies.csv` or `data/sessions.csv`) using a standard `FileWriter` truncates the file immediately. If an `IOException`, unexpected JVM termination, or power disruption occurs mid-write, the storage file is left truncated and corrupted.

### 2.2. Implementation in Codebase

`CsvUtil.writeToFile(Path targetPath, List<String[]> rows)` implements a resilient write protocol:

1. Ensure the destination directory exists and write all RFC 4180 escaped CSV rows (including the fixed header row) to a temporary sibling file (`<filename>.csv.tmp`), fully flushing the buffered UTF-8 stream.
2. Atomically swap the temporary file into place using `Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)`, falling back to `StandardCopyOption.REPLACE_EXISTING` if the underlying filesystem does not support atomic file replacement.

Each domain controller coordinates in-memory changes with `saveToCsv()` calls that delegate to `CsvUtil.writeToFile`, guaranteeing that files on disk represent complete, valid snapshots at all times.

---

## 3. Singleton Pattern

### 3.1. Context and Problem

In Java console applications, instantiating multiple `new Scanner(System.in)` objects across disparate view and utility classes produces input buffer collisions, skipped carriage returns, and `NoSuchElementException` errors. Furthermore, closing any wrapper `Scanner` permanently closes `System.in` for the entire JVM process.

### 3.2. Implementation in Codebase

`ScannerManager` implements a thread-safe, eager Singleton managing the single `Scanner(System.in, "UTF-8")` instance throughout the application lifecycle:

```java
public final class ScannerManager {

    private static final Scanner INSTANCE = new Scanner(System.in, "UTF-8");

    private ScannerManager() {
    }

    public static Scanner getInstance() {
        return INSTANCE;
    }
}
```

`ConsoleUtil` accesses the shared console stream exclusively through `ScannerManager.getInstance()`, ensuring synchronized, predictable line reading across all console views.

---

## 4. Strategy Pattern

### 4.1. Context and Problem

Different subscription tiers apply different monthly rates and streaming limits. `Standard` subscribers pay a basic subscription fee ($7.99/month) and are permitted 2 concurrent streams, whereas `Premium` subscribers pay a higher rate ($14.99/month) with 4 concurrent streams and 4K ultra-high-definition streaming access.

### 4.2. Implementation in Codebase

`Subscriber` declares abstract strategy methods:

```java
public abstract double calculateMonthlyFee();
public abstract int getMaxConcurrentStreams();
```

Concrete subclasses encapsulate the tier-specific calculations:

- `StandardSubscriber.calculateMonthlyFee()` returns `7.99` and `getMaxConcurrentStreams()` returns `2`.
- `PremiumSubscriber.calculateMonthlyFee()` returns `14.99` and `getMaxConcurrentStreams()` returns `4`.

Callers (such as `AnalyticsController` for monthly revenue projection) invoke `subscriber.calculateMonthlyFee()` polymorphically without branching on the tier string.

---

## 5. Template Method Pattern

### 5.1. Context and Problem

Every interactive console screen follows an identical navigation cycle: clear screen, render the centered 80-column breadcrumb banner, render the menu options (`[1]..[N]` and `[0]` Back/Exit), read a validated integer selection, dispatch the choice, and loop continuously until `0` is selected. Duplicating this control loop across six menu classes violates `DRY` and causes inconsistent terminal navigation.

### 5.2. Implementation in Codebase

The abstract class `Menu` defines the `final` template method `display()`:

```java
public final void display() {
    boolean running = true;
    while (running) {
        renderMenuFrame();
        int choice = ConsoleUtil.readInt("Enter your choice [0-" + this.options.length + "]: ", 0, this.options.length);
        if (choice == 0) {
            onExit();
            running = false;
        } else {
            ConsoleUtil.clearScreen();
            if (shouldShowBreadcrumb()) {
                printBreadcrumbHeader(this.title + " > [" + choice + "] " + this.options[choice - 1]);
            }
            handleChoice(choice);
        }
    }
}
```

Subclasses implement the abstract primitive operation `protected abstract void handleChoice(int choice)`:

- Each menu option executes a standardized continuous `while (true)` loop with distinct iteration dividers (`printDivider()` outputs an empty line followed by an 80-column horizontal divider).
- Entering `0` at the primary prompt immediately breaks out of the action loop and returns to the menu frame.
- Entering `0` or `N` during secondary confirmation cancels the individual operation (`[INFO] Operation cancelled.`) and continues the loop.
- `shouldShowBreadcrumb()` defaults to `true` to render the centered 80-column hierarchical breadcrumb banner before executing the action, and is overridden in `MainMenu` to return `false`.
