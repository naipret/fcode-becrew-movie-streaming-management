# Clean Code and Documentation Standards

This document defines the coding hygiene, method decomposition, defensive programming, exception handling, and Javadoc documentation rules enforced across the Movie Streaming Management System.

---

## 1. Named Constants Over Magic Literals

Hardcoded numeric literals and repeated format strings are prohibited inside method bodies. Every domain threshold, screen width, layout constraint, and date format string must be declared as a named `static final` constant in `UPPER_SNAKE_CASE`:

```java
public static final int SCREEN_WIDTH = 80;
public static final int DEFAULT_TRENDING_DAYS = 7;
public static final int DEFAULT_TOP_K_LIMIT = 10;
public static final double STANDARD_MONTHLY_FEE = 7.99;
public static final double PREMIUM_MONTHLY_FEE = 14.99;
public static final String DISPLAY_DATE_PATTERN = "dd/MM/yyyy";
public static final String DISPLAY_TIMESTAMP_PATTERN = "dd/MM/yyyy HH:mm";
```

---

## 2. Small, Focused Methods

Methods must perform a single logical task at one level of abstraction:

1. Keep methods concise (ideally under 35 lines) and limit control-flow nesting (`if`, `for`, `while`, `try`) to at most 3 levels.
2. Use guard clauses (early returns or early exception throws at the top of a method) to handle invalid inputs or boundary cases before the main execution path.
3. Whenever a block of code inside a method requires an explanation to understand its purpose, extract that block into a private helper method with a descriptive verb-phrase name.

---

## 3. Defensive Programming and Centralized Validation

### 3.1. Fail-Fast Invariant Validation via InputValidator

All stateless input normalization and domain validation rules reside in `fcode.becrew.moviestreamingmanagement.util.InputValidator`. Both `model` constructors or setters and `controller` entry points validate arguments through `InputValidator` immediately upon entry so invalid data can never corrupt in-memory collections or persistent CSV files.

### 3.2. Protecting Internal Collections

Classes that own internal `List`, `Set`, or `Map` collections (`MovieController.movies`, `CategoryController.categories`, `SubscriberController.subscribers`, `StreamingSessionController.sessions`) must never return mutable collection references from accessor methods. Always wrap returned collections using `Collections.unmodifiableList(...)`, `Collections.unmodifiableSet(...)`, or return defensive shallow copies.

### 3.3. Explicit Dependency Injection

Controllers and menus must receive their collaborators explicitly through constructor parameters wired in `Main.java`. Do not create default no-argument constructors in controllers or menus that silently instantiate duplicate controller instances in memory.

---

## 4. Exception Handling Discipline

1. Throw specific, standard JDK runtime exceptions when a contract or business rule is violated:
   - Use `IllegalArgumentException` for invalid field formats, negative durations, out-of-range ratings, duplicate titles, duplicate phone numbers, or missing referenced entities.
   - Use `IllegalStateException` when an operation is invoked in an invalid lifecycle state, such as recording a session against a deleted movie or encountering corrupted CSV records.
2. Never use empty catch blocks that swallow errors silently.
3. Controllers propagate exceptions upward to the `view` tier, where menu classes catch them and display clear `[ERROR] <message>` notifications to the operator.

---

## 5. Comment Policy and Plain-Text Javadoc

### 5.1. Zero Single-Line or Block Comments

Single-line comments (`// ...`) and inline block comments (`/* ... */`) are prohibited anywhere in `.java` source files. Code structure, domain names, and method composition must be completely self-documenting.

### 5.2. Plain-Text Javadoc Only

Every class, constructor, and non-trivial method must be documented using a standard Javadoc block (`/** ... */`) written in clear, plain English prose:

1. Do not use HTML tags inside Javadoc comments (`<p>`, `<ul>`, `<li>`, `<ol>`, `<b>`, `<i>`, `<br>`).
2. Do not use inline Javadoc markup tags (`{@code ...}`, `{@link ...}`) or HTML character entities (`&lt;`, `&gt;`, `&amp;`).
3. Include `@param`, `@return`, and `@throws` tags for all method parameters, return values, and thrown exceptions.
