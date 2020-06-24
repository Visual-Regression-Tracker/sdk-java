package io.visual_regression_tracker.sdk_java;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Example {
    VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(
            "http://localhost:4200",
            "733c148e-ef70-4e6d-9ae5-ab22263697cc",
            "BAZ0EG0PRH4CRQPH19ZKAVADBP9E",
            "develop"
    );

    @Test
    public void asdas() throws IOException {
        VisualRegressionTracker visualRegressionTracker = new VisualRegressionTracker(config);

        byte[] fileContent = FileUtils.readFileToByteArray(
                new File(
                        getClass()
                                .getClassLoader()
                                .getResource("home_page.png")
                                .getFile()
                ));

        visualRegressionTracker.track(
                "Java test default options",
                Base64.getEncoder().encodeToString(fileContent)
        );

        visualRegressionTracker.track(
                "Java test",
                Base64.getEncoder().encodeToString(fileContent),
                TestRunOptions.builder()
                        .device("Device")
                        .os("OS")
                        .browser("Browser")
                        .viewport("Viewport")
                        .diffTollerancePercent(5)
                        .build()
        );
    }
}
