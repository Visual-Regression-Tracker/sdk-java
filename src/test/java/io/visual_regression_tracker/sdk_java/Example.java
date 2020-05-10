package io.visual_regression_tracker.sdk_java;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Example {
    Config config = Config.builder()
            .apiUrl("http://localhost:4200")
            .branchName("develop")
            .projectId("003f5fcf-6c5f-4f1f-a99f-82a697711382")
            .token("F5Z2H0H2SNMXZVHX0EA4YQM1MGDD")
            .build();

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

        visualRegressionTracker.track(TestRun.builder()
                .name("Java test")
                .imageBase64(Base64.getEncoder().encodeToString(fileContent))
                .build());
    }
}
