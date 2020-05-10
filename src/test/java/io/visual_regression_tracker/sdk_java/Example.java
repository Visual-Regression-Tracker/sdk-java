package io.visual_regression_tracker.sdk_java;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Example {
    Config config = new Config(
            "http://localhost:4200",
            "003f5fcf-6c5f-4f1f-a99f-82a697711382",
            "F5Z2H0H2SNMXZVHX0EA4YQM1MGDD",
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
                "Java test",
                Base64.getEncoder().encodeToString(fileContent),
                TestRunOptions.builder().build()
        );
    }
}
