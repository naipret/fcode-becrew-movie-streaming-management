# Movie Streaming Management System

A console-based movie streaming platform administration system built with pure Java SE 8 for the `F-Code BeCrew` technical selection. The system manages movie catalogs across genres, subscriber profiles with tiered access rights, streaming session telemetry with concurrency validation, and analytical platform engagement reporting.

---

## 1. Technical Stack and Constraints

| Component | Specification |
| :--- | :--- |
| Language | Java SE 8 (Java Core 8, `javac.source=1.8`, `javac.target=1.8`) |
| Dependencies | Standard JDK 8 library only (Zero external third-party JARs or frameworks) |
| Runtime Compatibility | Standard Java 8+ runtime (OpenJDK, Eclipse Temurin, Oracle JDK) |
| User Interface | 80-column VT100 ASCII Console Text User Interface (English) |
| Data Persistence | RFC 4180 CSV flat files (`data/*.csv`) with atomic file replacement |

---

## 2. Project Structure

The codebase follows a standard 3-tier Model-View-Controller (`MVC`) architecture supplemented by stateless utility helpers:

```text
fcode-becrew-movie-streaming-management/
├── CONTRIBUTING.md
├── README.md
├── scripts/
│   ├── seed.ps1
│   └── seed.sh
├── data/
│   ├── categories.csv
│   ├── movies.csv
│   ├── subscribers.csv
│   └── sessions.csv
├── docs/
│   ├── BUSINESS_RULES.md
│   ├── ARCHITECTURE_MODEL.md
│   ├── DESIGN_PRINCIPLES.md
│   ├── DESIGN_PATTERNS.md
│   ├── UI_SPECIFICATION.md
│   ├── NAMING_CONVENTIONS.md
│   ├── CLEAN_CODE.md
│   └── WRITING_STYLE.md
└── src/fcode/becrew/moviestreamingmanagement/
    ├── Main.java
    ├── model/
    ├── controller/
    ├── view/
    └── util/
```

---

## 3. Generating Dynamic Demo Data

The application starts cleanly with empty header-only CSV files automatically. To populate a rich, practical demo dataset (8 categories, 20 movies, 15 subscribers across Standard and Premium tiers, 40 streaming sessions across past and present dates):

```powershell
# powershell
scripts\seed.ps1
```

```bash
# bash
./scripts/seed.sh
```

> [!NOTE]
> All session timestamps and subscriber activity records are calculated dynamically relative to the current local execution date (`today`). This guarantees that concurrency validation, engagement analytics, and Top-K ranking reports remain alive and testable at any point in time.

---

## 4. Continuous Automated Releases

On every push to `main`, the CI workflow automatically compiles and packages a single release archive:

- Release Asset: `fcode-becrew-movie-streaming-management.zip`
- Contents: Pre-compiled executable JAR in `dist/`, full source code in `src/`, dynamic seed scripts in `scripts/`, empty `data/` directory, and optional NetBeans IDE project metadata for convenient one-click importing.
- Latest Build: [GitHub Releases](https://github.com/naipret/fcode-becrew-movie-streaming-management/releases/tag/latest)

---

## 5. Live Interactive Web Terminal

Experience the complete application directly inside your web browser without installing any Java runtime:

- Runs 100% client-side via CheerpJ 3.0 WebAssembly JVM runtime and xterm.js with a GNOME Terminal dark theme.
- Features client-side dynamic seed data generation, ensuring live dates on every visit.
- For in-depth architectural details on CheerpJ JNI stream bridging and WebAssembly execution, refer to [Web Terminal Documentation](.github/pages/README.md).
- Live Web Terminal: [Github Pages](https://naipret.github.io/fcode-becrew-movie-streaming-management/)

---

## 6. Documentation Index

All architectural decisions, domain rules, and coding standards are maintained in the [`docs/`](docs/) directory:

1. [Business Rules](docs/BUSINESS_RULES.md): Invariants `BR1` through `BR35` governing genre categories, movie catalog, subscriber tiers, streaming sessions, analytical metrics, and soft deletes.
2. [Architecture and Data Model](docs/ARCHITECTURE_MODEL.md): 3-tier MVC package boundaries, UML class hierarchy, and CSV schemas.
3. [Design Principles](docs/DESIGN_PRINCIPLES.md): Four OOP pillars, SOLID principles, and core engineering principles (`SoC`, `DRY`, `KISS`, `YAGNI`).
4. [Design Patterns](docs/DESIGN_PATTERNS.md): Factory Method, Atomic CSV Persistence, Singleton, Strategy, and Template Method patterns.
5. [User Interface Specification](docs/UI_SPECIFICATION.md): 80-column ASCII terminal standards, continuous loops, status message prefixes, and table formatting.
6. [Naming Conventions](docs/NAMING_CONVENTIONS.md): Architectural naming conventions for classes, methods, variables, and constants.
7. [Clean Code Standards](docs/CLEAN_CODE.md): Defensive programming, exception handling, and Javadoc standards.
8. [Writing Style Guide](docs/WRITING_STYLE.md): Engineering prose standards, prohibited AI writing patterns, and typography rules.

---

## 7. License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
