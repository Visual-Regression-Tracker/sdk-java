# Java SDK for [Visual Regression Tracker](https://github.com/Visual-Regression-Tracker/Visual-Regression-Tracker)

## Gradle
```
repositories {
    maven { url 'https://jitpack.io' }
}
```
```
dependencies {
    implementation group: 'com.github.visual-regression-tracker', name: 'sdk-java', version: '${VERSION}'
}
```
## Maven
```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```
<dependency>
    <groupId>com.github.Visual-Regression-Tracker</groupId>
    <artifactId>sdk-java</artifactId>
    <version>${VERSION}</version>
</dependency>
```
[Available versions](https://github.com/Visual-Regression-Tracker/sdk-java/releases)

More info about https://jitpack.io/

## Usage
* Create config
```
Config config = new Config(
    "http://localhost:4200",
    "003f5fcf-6c5f-4f1f-a99f-82a697711382",
    "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD",
    "develop"
);
```
* Create an instance of `VisualRegressionTracker`
```
VisualRegressionTracker visualRegressionTracker = new VisualRegressionTracker(config);
```
* Take a screenshot as String in Base64 format
```
// Selenium example
String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
```
* Track image
```
visualKnightCore.processScreenshot(
        "Name for test",
        screenshotBase64,
        visualKnightCapabilities
);
visualRegressionTracker.track(
        "Name for test",
        screenshotBase64,
        TestRunOptions.builder()
            .browser("Chrome")
            .os("Windows")
            .viewport("1200x800")
            .diffTollerancePercent(1)
            .build()
);
```
