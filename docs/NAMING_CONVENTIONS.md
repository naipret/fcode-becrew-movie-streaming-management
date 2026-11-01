# Naming Conventions Specification

This document defines the naming rules for packages, classes, methods, variables, constants, and domain identifiers across the Movie Streaming Management System.

---

## 1. General Java Identifier Rules

| Element Type | Case Convention | Grammatical Form | Project Examples |
| :--- | :--- | :--- | :--- |
| Package | `lowercase` | Concatenated domain words without underscores | `fcode.becrew.moviestreamingmanagement.controller` |
| Class / Abstract Class | `PascalCase` | Singular noun or noun phrase | `Movie`, `Subscriber`, `StreamingSession`, `Menu`, `CsvUtil` |
| Method | `camelCase` | Imperative verb or verb phrase | `recordSession`, `calculateMonthlyFee`, `writeToFile` |
| Local Variable / Parameter | `camelCase` | Descriptive noun or noun phrase | `movieId`, `durationMinutes`, `watchedMinutes` |
| Constant (`static final`) | `UPPER_SNAKE_CASE` | Uppercase words separated by underscores | `SCREEN_WIDTH`, `DEFAULT_TOP_K_LIMIT`, `STANDARD_MONTHLY_FEE` |

Single-letter variable names are prohibited except for standard loop indices (`i`, `j`) inside short indexed loops.

---

## 2. Architectural Suffixes by Package

Every class outside the `model` package uses a suffix that identifies its architectural role at a glance:

| Package | Required Suffix | Responsibility | Examples |
| :--- | :--- | :--- | :--- |
| `model` | None (Domain Noun) | Represents a domain entity or value record | `Movie`, `Category`, `Subscriber`, `StandardSubscriber`, `PremiumSubscriber`, `StreamingSession` |
| `controller` | `*Controller` | Coordinates business rules, collection queries, and CSV persistence | `MovieController`, `CategoryController`, `SubscriberController`, `StreamingSessionController`, `AnalyticsController` |
| `view` | `*Menu` | Renders an 80-column interactive console menu screen | `Menu`, `MainMenu`, `MovieMenu`, `CategoryMenu`, `SubscriberMenu`, `StreamingSessionMenu`, `AnalyticsMenu` |
| `util` | `*Util`, `*Validator`, `*Formatter`, or `*Manager` | Provides stateless helper routines or singleton stream management | `CsvUtil`, `InputValidator`, `ConsoleUtil`, `TableFormatter`, `ScannerManager` |

---

## 3. Method Verb and Boolean Prefix Conventions

Method names must begin with a verb or prefix that accurately reflects whether the method queries state, mutates state, or transforms input:

| Prefix | Contract and Return Type | Project Examples |
| :--- | :--- | :--- |
| `is*`, `has*`, `should*` | Side-effect-free predicate returning `boolean` | `isCompleted()`, `isDeleted()`, `shouldShowBreadcrumb()` |
| `get*` | Accessor or computed query returning a value or unmodifiable collection | `getWatchedMinutes()`, `getDurationMinutes()`, `getMovies()` |
| `set*` | Validated field mutator returning `void` | `setTitle(String)`, `setRating(double)`, `setDeleted(boolean)` |
| `find*` | Single-entity lookup returning an entity or `null` if not found | `findById(String)`, `findByIdIncludeDeleted(String)`, `findByPhone(String)` |
| `search*` | Multi-entity query returning a `List<T>` (empty list if no matches) | `searchByKeyword(String)`, `searchByReleaseYear(int)` |
| `calculate*` | Mathematical computation of metrics or subscription fees | `calculateMonthlyFee()`, `calculateCompletionRate()` |
| `validate*` | Stateless invariant check throwing `IllegalArgumentException` on failure | `validatePositiveDuration(int)`, `validateRating(double)`, `validatePhone(String)` |
| `normalize*` | Trims, formats, validates, and returns a canonical string | `normalizeId(String, char)`, `normalizePhone(String)`, `normalizeTitle(String)` |
| `load*` / `save*` | Reads or writes persistent CSV snapshots | `loadFromCsv()`, `saveToCsv()` |

---

## 4. Domain Identifier Formatting Rules

All primary entity identifiers follow a uniform 6-character uppercase pattern consisting of a 1-letter entity prefix followed by a 5-digit zero-padded number (`00001` through `99999`):

| Entity | Prefix | Canonical Format | Shorthand Input Accepted by `InputValidator.normalizeId` |
| :--- | :--- | :--- | :--- |
| `Movie` | `M` | `M00001` - `M99999` | `"1"`, `"m1"`, `"M01"` normalize to `"M00001"` |
| `Category` | `G` | `G00001` - `G99999` | `"1"`, `"g1"`, `"G01"` normalize to `"G00001"` |
| `Subscriber` | `U` | `U00001` - `U99999` | `"1"`, `"u1"`, `"U01"` normalize to `"U00001"` |
| `StreamingSession` | `S` | `S00001` - `S99999` | `"1"`, `"s1"`, `"S01"` normalize to `"S00001"` |

---

## 5. Compliant vs. Non-Compliant Examples

| Category | Non-Compliant Name | Compliant Name | Reason |
| :--- | :--- | :--- | :--- |
| Class Name | `Film`, `CategoryHelper`, `AbstractMenu` | `Movie`, `CategoryController`, `Menu` | Aligns with streaming domain terminology and standard architectural suffixes |
| Boolean Method | `checkCompleted()`, `completedStatus()` | `isCompleted()` | Boolean predicates must start with `is`, `has`, or `should` |
| Identifier Query | `getMovie()` | `findById(String id)` | Single-entity lookup by identifier uses `find*` prefix |
| Constant | `int limit = 10;` | `public static final int DEFAULT_TOP_K_LIMIT = 10;` | Replaces magic number with a descriptive `UPPER_SNAKE_CASE` constant |
| Collection Variable | `List<Movie> list` | `List<Movie> activeMovies` | Describes the domain contents of the collection |
