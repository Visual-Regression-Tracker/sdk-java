# Java SDK for [Visual Regression Tracker](https://github.com/Visual-Regression-Tracker/Visual-Regression-Tracker)

Track screenshots from Java tests in a Visual Regression Tracker server. Requires Java 17 or later and a VRT server 6.0 or later.

## Install

The SDK is published on [JitPack](https://jitpack.io/#Visual-Regression-Tracker/sdk-java). The version is a git tag; see [releases](https://github.com/Visual-Regression-Tracker/sdk-java/releases).

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    testImplementation 'com.github.Visual-Regression-Tracker:sdk-java:6.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Visual-Regression-Tracker</groupId>
    <artifactId>sdk-java</artifactId>
    <version>6.0.0</version>
    <scope>test</scope>
</dependency>
```

The SDK logs through SLF4J 2. Add a provider such as `logback-classic` to see its output.

### Upgrading to 6.0

- Java 17 or later is required.
- SLF4J 1.7 bindings are not picked up; use an SLF4J 2 provider (logback 1.3+, `log4j-slf4j2-impl`).
- The API is unchanged. A call that passes a literal `null` or a bare Mockito `any()` as the image is now ambiguous between the `String`, `byte[]` and `Path` overloads of `track`; use `any(String.class)`.

## Configuration

| Option | Environment variable | Required | Description |
|---|---|---|---|
| `apiUrl` | `VRT_APIURL` | yes | URL of the VRT API, for example `http://localhost:4200` |
| `apiKey` | `VRT_APIKEY` | yes | API key of a VRT user |
| `project` | `VRT_PROJECT` | yes | Project name or ID |
| `branchName` | `VRT_BRANCHNAME` | no | Branch of the build. If not set, the build gets the project's main branch |
| `sourceBranch` | `VRT_SOURCEBRANCH` | no | Branch whose baselines a test without a baseline on `branchName` is compared against. If not set, the project's main branch |
| `ciBuildId` | `VRT_CIBUILDID` | no | ID of the CI build. Starting a build with an ID that already exists continues that build |
| `enableSoftAssert` | `VRT_ENABLESOFTASSERT` | no | Log `new` and `unresolved` results instead of throwing. Default `false` |
| `httpTimeoutInSeconds` | `VRT_HTTPTIMEOUTINSECONDS` | no | How long to wait for each response. Default `360`, because the server compares images while it handles the request; `0` means no limit. Connecting has its own 10 second limit |

Each option is taken from the builder (or constructor) first, then the environment variable, then `vrt.json` in the working directory (or the file passed to `configFile`).

### Builder

```java
VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
        .apiUrl("http://localhost:4200")
        .apiKey("F5Z2H0H2SNMXZVHX0EA4YQM1MGDD")
        .project("Default project")
        .branchName("feature/login")
        .sourceBranch("develop")
        .enableSoftAssert(true)
        .build();
```

### Environment variables

```bash
export VRT_APIURL=http://localhost:4200
export VRT_APIKEY=F5Z2H0H2SNMXZVHX0EA4YQM1MGDD
export VRT_PROJECT="Default project"
export VRT_BRANCHNAME=feature/login
export VRT_SOURCEBRANCH=develop
export VRT_CIBUILDID=40bdba4
export VRT_ENABLESOFTASSERT=true
```

### `vrt.json`

```json
{
  "apiUrl": "http://localhost:4200",
  "apiKey": "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD",
  "project": "Default project",
  "branchName": "feature/login",
  "sourceBranch": "develop",
  "ciBuildId": "40bdba4",
  "enableSoftAssert": true
}
```

Keep the API key out of git: commit a `vrt.json.example` and ignore `vrt.json`.

## Usage

```java
VisualRegressionTracker vrt = new VisualRegressionTracker(config);
// or new VisualRegressionTracker() to read the environment and vrt.json only

vrt.start();
try {
    // Selenium: a file, bytes or base64
    Path screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
    vrt.track("Login page", screenshot, TestRunOptions.builder()
            .browser("Chrome")
            .os("Linux")
            .viewport("1280x720")
            .build());
} finally {
    vrt.stop();
}
```

`start()` creates a build; every `track` call adds a test run to it; `stop()` finishes the build.

### Image

| Call | Upload |
|---|---|
| `track(name, Path image)` | multipart, file name kept |
| `track(name, byte[] image)` | multipart |
| `track(name, String imageBase64)` | base64 in JSON |

Each also takes a `TestRunOptions` as the third argument. Prefer a file or bytes: base64 in JSON is bound by the server's `BODY_PARSER_JSON_LIMIT` (5 MB by default), multipart by the much larger `MULTIPART_FILE_SIZE_LIMIT`.

### `TestRunOptions`

| Option | Description |
|---|---|
| `os`, `browser`, `viewport`, `device`, `customTags` | Part of the test's identity: a baseline is matched on name and all of these |
| `diffTollerancePercent` | Difference, in percent, still treated as a match (the spelling is the server's) |
| `ignoreAreas` | List of `IgnoreAreas` (`x`, `y`, `width`, `height`) left out of the comparison |
| `comment` | Comment shown on the test run |
| `merge` | Mark the test run for merging into the main branch |

## Result and statuses

`track` returns a `TestRunResult`:

- `getTestRunResponse()`: `getId()`, `getStatus()`, `getUrl()` (the test run in the VRT UI), `getDiffPercent()` and the rest of the server's answer;
- `getImageUrl()`, `getBaselineUrl()`, `getDiffUrl()`: the images, served through `<apiUrl>/images/<name>`; `null` when there is no such image.

| Status | `track` |
|---|---|
| `OK`, `APPROVED`, `AUTO_APPROVED`, `FAILED` | returns |
| `NEW` (no baseline yet) | throws `TestRunException` "No baseline: <url>", or logs it with `enableSoftAssert` |
| `UNRESOLVED` (difference found) | throws `TestRunException` "Difference found: <url>", or logs it with `enableSoftAssert` |
| `UNKNOWN` (a status this SDK does not know) | logs a warning and returns |

## Errors

When the server rejects a request, the SDK throws `TestRunException`. `getStatusCode()` holds the HTTP status and the message reads `Request failed with status <code>: <server's message>`. Calling `track` or `stop` before `start` also throws it, with no status code.

| Status | Meaning |
|---|---|
| 401, 403 | Wrong `apiKey`, or the user may not use the project |
| 404 | Project or build not found |
| 413 | Request too large. For base64 the message suggests multipart; for multipart it names `MULTIPART_FILE_SIZE_LIMIT` |
| 422 | Image has more pixels than the server's `MAX_IMAGE_PIXELS` |
| 503 | Server busy. With `Retry-After` the SDK waits and retries up to 3 times (at most 60 s per wait) before throwing |

Network failures and timeouts surface as `IOException` (`HttpTimeoutException` for a timeout).

## Releasing

JitPack builds the SDK from a git tag; there is nothing to upload.

1. Make sure `master` is green.
2. Tag the commit with the version, without a `v`: `git tag <version> && git push origin <version>`.
3. Create a GitHub release for the tag with the release notes.
4. Open `https://jitpack.io/#Visual-Regression-Tracker/sdk-java/<version>` to start the build, and check that its log ends with the artifact list.

JitPack runs `./gradlew clean -Pgroup=com.github.Visual-Regression-Tracker -Pversion=<tag> -xtest assemble publishToMavenLocal` on the JDK from `jitpack.yml`, so the artifact version is always the tag. `version` in `gradle.properties` is only the default for local builds. CI runs the same command on Java 17 for every pull request.

## Links

- [Java examples](https://github.com/Visual-Regression-Tracker/examples-java)
- [Other SDKs and agents](https://github.com/Visual-Regression-Tracker)

## License

[Apache-2.0](LICENSE)
