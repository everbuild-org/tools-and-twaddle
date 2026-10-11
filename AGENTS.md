# Working on Tools & twaddle

Tools & twaddle is an experimental Kotlin/JVM toolkit for composing Minestom servers.
Read `README.md`, `CONTRIBUTING.md`, and the relevant module's code before changing behavior.
For work under `docs/`, also follow `docs/AGENTS.md`.

## Project map

- `modules/twaddle-lifecycle`: application lifetime, resource ownership, and coroutine scopes.
- `modules/twaddle-structured-logging`: SLF4J logging helpers; applications choose the logging backend.
- `modules/twaddle-stacked-behaviour`: typed activities, ordered rule chains, and configuration.
- `modules/twaddle-stacked-behaviour-block`: Minestom block handlers built on stacked behaviour.
- `modules/twaddle-transfer-rules`: inventory shift-click routing.
- `modules/twaddle-random-ticking`: sampled block updates.
- `common/twaddle-common-minestom`: event and packet interoperability helpers.
- `common/twaddle-common-block`: shared block contracts.
- `common/twaddle-testing`: shared test assertions and Minestom environment fixtures.
- `testserver/`: Kotlin and Java integration examples; not a published library.
- `buildSrc/`: shared Kotlin JVM and Maven publishing convention plugins.
- `docs/`: Astro/Starlight documentation, with pages in `src/content/docs/`.

## Architecture and code style

- Keep Minestom primitives visible. Applications create their servers and compose the modules they need.
- Keep gameplay features optional. Do not require lifecycle ownership or a central module registry for independent features.
- Put APIs with the module that owns the concept. Use common modules for genuinely shared contracts.
- Preserve dependency direction: block behavior builds on general stacked behavior and common block APIs;
  random ticking uses common block APIs without requiring stacked behavior.
- Follow existing Kotlin conventions and package names under `org.everbuild.twaddle`.
  Artifact names use hyphens; several existing package names use underscores.
- Prefer explicit data flow, small concrete types, and functional primitives where they simplify the code.
  Avoid speculative abstractions and unrelated refactors.
- Keep public APIs usable from Java. Follow existing builder and `@JvmStatic` patterns where needed;
  check Java call sites when changing Kotlin APIs exposed to Java.
- Document public contracts and non-obvious ownership or ordering rules. Avoid comments that restate the code.
- Preserve lifecycle cancellation, resource cleanup, and failure propagation semantics when changing ownership code.

## Gradle and dependencies

- Use the checked-in `./gradlew` wrapper. The project and `buildSrc` use a Java 25 toolchain.
- Manage dependency versions in `gradle/libs.versions.toml` and the shared release version in
  `gradle.properties` (`twaddle.version`). Avoid duplicating versions in module build files.
- Library modules apply `buildsrc.convention.kotlin-jvm` and `buildsrc.convention.publishing`.
  Give new libraries a meaningful Gradle `description` for their Maven POM.
- `settings.gradle.kts` automatically includes immediate children of `common/` and `modules/`
  that contain a `build.gradle.kts`.
- Use type-safe project accessors for dependencies inside this repository.
  Match the module's existing `api`, `implementation`, and `compileOnly` boundaries;
  most libraries expect the consuming application to supply Minestom.

## Verification

Run commands from the repository root unless stated otherwise:

```sh
./gradlew build
# Narrow a JVM change to its module while iterating:
./gradlew :modules:twaddle-stacked-behaviour:build
# Build the documentation:
pnpm --dir docs install --frozen-lockfile
pnpm --dir docs build
git diff --check
```

- JVM CI runs `./gradlew build`; use it for changes across modules or shared build conventions.
- Follow the nearby test style. Kotlin suites use TestBalloon and Kotest assertions;
  JUnit Jupiter and Mockito are also configured, including for Java tests.
- For Minestom environment tests, reuse `envTest` in `common/twaddle-testing` rather than duplicating
  process initialization and cleanup. Add `testImplementation(projects.common.twaddleTesting)` when needed.
- Cover changed observable behavior and relevant regressions. Test Java interop when changing Java-facing APIs.
- JaCoCo XML and HTML reports are configured in the JVM convention plugin; CI uploads coverage to Codecov.
- For documentation-only changes, build the docs. Report checks performed and any limitations;
  do not describe a dry run as a successful publication or runtime test.

## Documentation and publishing

- Keep public API changes synchronized with the corresponding guide and Kotlin/Java examples.
  Update `docs/astro.config.mjs` when adding a page that belongs in the sidebar.
- Consumer examples use `org.everbuild.twaddle:<project-name>:<version>`, not Gradle project accessors.
  Keep example versions consistent with the version they document and distinguish configured versions from published releases.
- The public Maven repository is named `asordaPublic` at `https://mvn.everbuild.org/public`.
  Consumers do not need publishing credentials. Documentation lives at `https://twaddle.asorda.net`.
- Publishing credentials come from `MVN_TWADDLE_USER` and `MVN_TWADDLE_PASSWORD`.
  Never commit credentials or print their values.
- `.github/workflows/release.yml` is manually dispatched. It reads `twaddle.version`, builds and tests
  the `v<version>` tag, pushes the tag, publishes libraries, and creates a GitHub release with generated notes
  and dependency setup instructions. Maven versions omit the tag's `v` prefix.
- Release validation rejects existing tags and snapshot versions. A failure after the tag is pushed requires
  inspecting remote artifacts before recovery; do not overwrite published versions as a routine fix.
- To validate the publishing task graph without uploading artifacts:
  `./gradlew publishAllPublicationsToAsordaPublicRepository --dry-run --no-configuration-cache`.
  Do not run a real publication or dispatch a release merely to verify an unrelated change.

## Change hygiene

- Preserve existing user edits and keep changes scoped to the task.
- Follow Conventional Commits when making commits. PR descriptions should explain the behavior change,
  validation, and AI assistance as required by `CONTRIBUTING.md`.
- Do not commit generated build output, dependency directories, local IDE state, or secrets.
