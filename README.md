# Java SDK for [Visual Regression Tracker](https://github.com/Visual-Regression-Tracker/Visual-Regression-Tracker)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/79dcd96f2be04992bc1059cad60e2e04)](https://www.codacy.com/gh/Visual-Regression-Tracker/sdk-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Visual-Regression-Tracker/sdk-java&amp;utm_campaign=Badge_Grade)

## Gradle

```yml
repositories {
    maven { url 'https://jitpack.io' }
}
```

```yml
dependencies {
    implementation group: 'com.github.visual-regression-tracker', name: 'sdk-java', version: '${REPLACE_THIS_VALUE}'
}
```

## Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.Visual-Regression-Tracker</groupId>
    <artifactId>sdk-java</artifactId>
    <version>${REPLACE_THIS_VALUE}</version>
</dependency>
```
[Available versions](https://github.com/Visual-Regression-Tracker/sdk-java/releases)

More info about https://jitpack.io/

## Usage

* Create config

```java
VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(
    // apiUrl - URL where backend is running 
    "http://localhost:4200",
    
    // project - Project name or ID
    "003f5fcf-6c5f-4f1f-a99f-82a697711382",
    
    // apiKey - User apiKey
    "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD",
    
    // branch - Current git branch 
    "develop",
    
    // enableSoftAssert - Log errors instead of exceptions
    false,
 
    // ciBuildId - id of the build in CI system
    "CI_BUILD_ID",
    
    // httpTimeoutInSeconds - define http socket timeout in seconds (default 10s)
    15

);
```

* Create an instance of `VisualRegressionTracker`

```java
VisualRegressionTracker visualRegressionTracker = new VisualRegressionTracker(config);
```

* Take a screenshot as String in Base64 format

```java
// Selenium example
String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
```

* Track image

Default options

```java
visualRegressionTracker.track(
        "Name for test",
        screenshotBase64
);
```

With specific options 

```java
visualRegressionTracker.track(
        "Name for test",
        screenshotBase64,
        TestRunOptions.builder()
            .browser("Chrome")
            .os("Windows")
            .viewport("1200x800")
            .diffTollerancePercent(3.0f)
            .build()
);
```
