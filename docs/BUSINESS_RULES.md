# Business Rules Specification

This document defines the domain invariants, validation constraints, streaming session logic, and transactional policies governing the Movie Streaming Management System.

---

## 1. Movie Catalog and Metadata Invariants

| Rule Name | Specification |
| :--- | :--- |
| Immutable Unique Movie Identifier | Each Movie ID is generated sequentially by the system (`M00001` through `M99999`) and remains permanently immutable. Lookups and search inputs accept shorthand formats (such as `M1` or `1`) and normalize them automatically to canonical format. |
| Non-Empty Title | Movie title must not be empty or consist solely of whitespace. Leading and trailing whitespace is trimmed. |
| Valid Category Association | Every movie must be associated with an existing, active category identifier. Associating a movie with a nonexistent or soft-deleted category is prohibited. |
| Positive Movie Duration | Movie duration must be a strictly positive integer representing total runtime in minutes (`durationMinutes > 0`). |
| Normalized Movie Rating Range | The critic rating must fall within the closed interval from 0.0 to 10.0 inclusive (`0.0 <= rating <= 10.0`). |
| Release Year Boundary | Movie release year must be between 1888 (the year the first motion picture was created) and the current calendar year plus five years to accommodate pre-announced catalog entries. |
| Non-Negative Cumulative View Count | Total view count must be a non-negative integer (`viewCount >= 0`). View counts increment automatically when playback sessions are recorded. |
| Unique Active Movie Title Check | Active movie titles must be unique across the catalog (case-insensitive) to prevent accidental duplicate entries. |

---

## 2. Category Registry and Referential Invariants

| Rule Name | Specification |
| :--- | :--- |
| Immutable Unique Category Identifier | Each Category ID is generated sequentially by the system (`G00001` through `G99999`) and remains permanently immutable. Lookups accept shorthand formats (such as `G1` or `1`) and normalize them automatically. |
| Unique Category Name | Category names must be non-empty and case-insensitively unique across all active categories in the registry. |
| Foreign Key Deletion Protection | A category cannot be deleted if active, non-deleted movies currently reference its Category ID. All referencing movies must be deleted or reassigned before the category can be removed. |

---

## 3. Subscriber Identity and Tier Invariants

| Rule Name | Specification |
| :--- | :--- |
| Immutable Unique Subscriber Identifier | Each Subscriber ID is generated sequentially by the system (`U00001` through `U99999`) and cannot be altered. Lookups accept shorthand formats (such as `U1` or `1`) and normalize them automatically. |
| Normalized Customer Full Name | Subscriber full names must be non-empty, sanitized against prohibited symbols or numbers, and normalized into standard Title Case (for example, `Nguyen Van An`). |
| Unique 10-Digit Phone Number | Subscriber phone numbers must be valid 10-digit mobile numbers matching the pattern `^0[35789]\d{8}$`. Active subscriber phone numbers must be unique across the entire subscriber registry. |
| Valid Email Address | Subscriber emails must be non-empty and conform to standard RFC 5322 email syntax containing valid local and domain parts. |
| Tier Selection and Polymorphism | Subscribers must be instantiated as either `Standard` or `Premium` tiers. Standard subscribers are billed $7.99/month and permitted 2 concurrent streams; Premium subscribers are billed $14.99/month and permitted 4 concurrent streams. |

---

## 4. Streaming Session and Viewing History Invariants

| Rule Name | Specification |
| :--- | :--- |
| Immutable Unique Session Identifier | Each Streaming Session ID is generated sequentially by the system (`S00001` through `S99999`) upon logging and cannot be altered. |
| Session Entity Referential Integrity | A streaming session must reference an existing active subscriber and an existing active movie. Recording sessions for soft-deleted subscribers or soft-deleted movies is rejected. |
| Positive Watched Minutes | Watched minutes in any playback session must be a strictly positive integer (`watchedMinutes > 0`). Watched minutes cannot exceed the total duration of the referenced movie plus a 30-minute allowance for rewatching segments. |
| Automatic Playback Completion Flag | If the recorded watched minutes equal or exceed the total runtime of the movie (`watchedMinutes >= movie.durationMinutes`), the session is automatically marked as complete (`isCompleted = true`). |
| View Count Synchronization | Recording a valid streaming session automatically increments the cumulative `viewCount` of the referenced movie by one. Cancelling an erroneous session decrements the view count by one. |
| Session Timestamp Invariant | Streaming session timestamps must not be set to future dates relative to current system time. |
| Concurrent Streaming Limit Enforcement | Active concurrent streams for a subscriber cannot exceed their subscription tier capacity (2 concurrent streams for Standard tier, 4 concurrent streams for Premium tier) at the time of session recording. |

---

## 5. Platform Analytics and Ranking Invariants

| Rule Name | Specification |
| :--- | :--- |
| Weighted Max-Heap Movie Ranking | The top-ranked movies are computed using a normalized multi-factor scoring formula: `Score = (0.5 * NormalizedRating) + (0.3 * LogNormalizedViews) + (0.2 * CompletionRate)`. Extraction uses a bounded `PriorityQueue` (Max-Heap) to retrieve Top-K movies in optimal time. |
| Sliding Window Trending Categories | Trending categories are calculated dynamically by aggregating all playback session durations recorded within a sliding time window (default 7 days) relative to the current local execution timestamp. |
| Monthly Performance Metric Aggregation | Monthly performance reports aggregate active subscribers, total sessions, cumulative streaming hours, average completion rate, and projected monthly subscription revenue for a given calendar month (`MM/yyyy`). |

---

## 6. Storage, Soft-Delete, and Concurrency Invariants

| Rule Name | Specification |
| :--- | :--- |
| Soft Deletion Protocol | Deleting a movie, category, or subscriber flags its `isDeleted` attribute as `true` instead of purging the CSV row. Soft-deleted identifiers remain permanently reserved to preserve foreign key referential integrity across past streaming sessions. |
| Atomic File Replacement Protocol | All updates to `data/*.csv` files write first to a temporary sibling file (`<file>.csv.tmp`), flush all buffers completely, and replace the destination file atomically using OS-level atomic move semantics. |
| RFC 4180 Escaping Compliance | Any text containing commas, line breaks, or quotation marks must be escaped using standard double quotation marks in CSV files. |
| 7-Bit ASCII Guarantee | All persistent records and console outputs are constrained to standard 7-bit ASCII characters to prevent encoding corruption across differing terminal environments and platforms. |
