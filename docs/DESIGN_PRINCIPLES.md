# Software Design Principles Specification

This document defines the software engineering principles, object-oriented programming pillars, and structural rules implemented across the Movie Streaming Management System.

---

## 1. The Four Pillars of OOP

### 1.1. Encapsulation

All entity state is encapsulated using private instance variables. Internal collections and mutable fields are protected from unauthorized external modification:

- Every field in `Movie`, `Category`, `Subscriber`, and `StreamingSession` is declared `private`.
- Mutators enforce validation invariants through `InputValidator` before updating internal state.
- Getters for collections wrap internal lists using `Collections.unmodifiableList(...)` to prevent callers from altering controller state without going through verified business methods.

### 1.2. Inheritance

Inheritance hierarchies represent genuine "is-a" relationships and eliminate redundant boilerplate:

- `Subscriber` serves as an abstract base class encapsulating common subscriber attributes (`id`, `fullName`, `phone`, `email`, `isDeleted`), extended by concrete subclasses `StandardSubscriber` and `PremiumSubscriber`.
- `Menu` provides an abstract base class that encapsulates the standardized 80-column menu frame rendering, continuous loop lifecycle, screen clearing, and breadcrumb header handling for all interactive terminal views.

### 1.3. Polymorphism

Polymorphic behavior enables flexible runtime dispatch without brittle type-checking or conditional branching:

- `Subscriber.calculateMonthlyFee()` and `Subscriber.getMaxConcurrentStreams()` are declared abstract in `Subscriber`. Callers calculate billings and verify streaming concurrency against the abstract `Subscriber` reference without inspecting concrete subclass types.
- The `Menu.handleChoice(int choice)` method is dispatched polymorphically by the template method `Menu.display()`, executing screen-specific workflows while preserving consistent navigation mechanics.

### 1.4. Abstraction

Abstraction exposes only essential interfaces to callers while concealing internal complexity:

- The `view` tier interacts with controllers solely through well-defined public operational methods (`recordSession`, `updateMovie`, `searchByKeyword`), remaining unaware of underlying CSV structures or atomic file replacement algorithms.
- `CsvUtil` abstracts file system I/O, character escaping, and OS-level atomic file replacement behind a simple `writeToFile(Path, List<String[]>)` interface.

---

## 2. SOLID Principles

### 2.1. Single Responsibility Principle (SRP)

Every class possesses a single, well-defined reason to change:

- `MovieController` coordinates movie catalog business rules and CSV persistence, remaining completely free of terminal input and output logic.
- `MovieMenu` handles console layout, user prompts, input reading, and exception display, delegating all domain calculations and persistence to `MovieController`.
- `CsvUtil` handles RFC 4180 parsing and atomic file writing, without knowledge of movie streaming domain rules.

### 2.2. Open/Closed Principle (OCP)

The software architecture is open for extension but closed for modification:

- New subscriber tiers (such as `StudentSubscriber` or `FamilySubscriber`) can be introduced by subclassing `Subscriber` and providing tier-specific rates without altering existing controller or view code.
- Movie filtering in `MovieController` accepts arbitrary combinations of Java 8 `Predicate<Movie>`, allowing new query filters without modifying catalog evaluation methods.

### 2.3. Liskov Substitution Principle (LSP)

Subclasses can substitute for their base class without altering the correctness of the program:

- `StandardSubscriber` and `PremiumSubscriber` fully honor the invariants established by `Subscriber`. Neither subclass weakens preconditions or strengthens postconditions of `calculateMonthlyFee()` or `getMaxConcurrentStreams()`.

### 2.4. Interface Segregation Principle (ISP)

Clients should not be forced to depend upon methods they do not use:

- Controllers expose distinct, focused public methods rather than large monolithic interfaces. For example, `AnalyticsController` only queries historical sessions and active movies without exposing mutation operations.

### 2.5. Dependency Inversion Principle (DIP)

High-level modules do not directly instantiate their low-level collaborators:

- Collaborators are passed explicitly via constructor parameters in `Main.java`. Controllers receive their dependent repositories and sibling controllers through explicit dependency injection, facilitating modularity and testing.

---

## 3. Core Software Engineering Principles

### 3.1. Separation of Concerns (SoC)

The system enforces strict vertical boundaries between the presentation tier (`view`), business coordination tier (`controller`), domain state tier (`model`), and stateless infrastructure (`util`). Presentation code never performs file I/O or domain calculations, while controllers never invoke `System.out` or `Scanner`.

### 3.2. Don't Repeat Yourself (DRY)

Shared algorithms, validation rules, and terminal formatting logic are consolidated in centralized utilities:

- Atomic file replacement logic is implemented once in `CsvUtil.writeToFile`.
- String padding, line truncating, and border generation are centralized in `TableFormatter`.
- Identifier format checking and phone normalization are located exclusively in `InputValidator`.

### 3.3. Keep It Simple, Stupid (KISS)

The system relies strictly on standard Java SE 8 constructs (`List`, `Map`, `Set`, `PriorityQueue`) without introducing heavy external frameworks, complex reflection, or redundant architectural layers.

### 3.4. You Aren't Gonna Need It (YAGNI)

Artificial mechanisms that do not serve real platform administration needs—such as user-facing video streaming animation, consumer watchlist management, and simulated player controls—have been eliminated in favor of a lean, focused administrative management core.
