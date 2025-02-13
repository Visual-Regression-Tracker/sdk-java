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

### Configuration
In order to initialize VisualRegressionTracker, following options should be defined:

* `apiUrl` (**Required**) - URL where backend is running. _Example_: "http://localhost:4200"
* `project` (**Required**) - Project name or ID. _Example_: "003f5fcf-6c5f-4f1f-a99f-82a697711382"
* `apiKey` (**Required**) - User apiKey. _Example_: "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD"
* `branch` (Optional) - Current git branch. _Example_: "develop"
* `enableSoftAssert` (Optional) - Log errors instead of exceptions. Default value is false
* `ciBuildId` (Optional) - id of the build in CI system
* `httpTimeoutInSeconds` (Optional) - define http socket timeout in seconds. Default value is 10 seconds

 There are a few ways to provide those options

<details>

<summary>Create config with builder</summary>

```java
VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
                .apiUrl("http://localhost:4200")
                .apiKey("F5Z2H0H2SNMXZVHX0EA4YQM1MGDD")
                .project("003f5fcf-6c5f-4f1f-a99f-82a697711382")
                .enableSoftAssert(true)
                .branchName("develop")
                .build();
```

</details>

<details>

<summary>Set environment variables</summary>

```
export VRT_APIURL=http://localhost:4200
export VRT_APIKEY=F5Z2H0H2SNMXZVHX0EA4YQM1MGDD
export VRT_PROJECT=003f5fcf-6c5f-4f1f-a99f-82a697711382
export VRT_BRANCHNAME=develop
export VRT_ENABLESOFTASSERT=true
export VRT_CIBUILDID=40bdba4
export VRT_HTTPTIMEOUTINSECONDS=15

```

</details>

<details>

<summary>Create vrt.json file in the root of the project</summary>

```json
{
  "apiUrl": "[http://162.243.161.172:4200](http://localhost:4200)",
  "project": "003f5fcf-6c5f-4f1f-a99f-82a697711382",
  "apiKey": "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD",
  "branchName": "deveolop",
  "enableSoftAssert": false,
  "ciBuildId": "40bdba4"
}

```

</details>

> [!NOTE]
> Final values, that will be used by VisualRegressionTracker, will be resolved as following:
> 1. If explicitly set while creating `VisualRegressionTrackerConfig` - use this value
> 2. Use value from environment variable if it exists
> 3. Try to get it from the configuration file

<br />


### Create an instance of `VisualRegressionTracker`

```java
VisualRegressionTracker visualRegressionTracker = new VisualRegressionTracker(config);
```

or

```java
VisualRegressionTracker visualRegressionTracker = new VisualRegressionTracker();
```

> [!TIP]
> If config is not provided explicitly, it will be created based on the environment variables or configuration file. Please see [Configuration](README.md#configuration) section

<br />

### Start `VisualRegressionTracker`

```java
visualRegressionTracker.start();
```

At that point VisualRegressionTracker will try to create a new build for provided project. All of the subsequent tracked screenshots are going to be included in that build.

<br />

### Take a screenshot as String in Base64 format

```java
// Selenium example
String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
```

<br />

### Track image

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


### Stop `VisualRegressionTracker`

```java
visualRegressionTracker.stop();
```

Should be called once current build should be considered as completed.
