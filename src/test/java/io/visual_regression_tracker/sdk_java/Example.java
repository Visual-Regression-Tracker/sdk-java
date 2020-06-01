package io.visual_regression_tracker.sdk_java;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Example {
    Config config = new Config(
            "http://localhost:4200",
            "90e3b95d-6468-4771-a2e7-7bc7d3ca2b1b",
            "W5KJ9ZGXRV458AH1XZEJ8WF284ED",
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
