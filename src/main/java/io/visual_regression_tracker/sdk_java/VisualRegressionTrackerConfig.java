package io.visual_regression_tracker.sdk_java;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisualRegressionTrackerConfig {
    private String apiUrl;
    private String project;
    private String apiKey;
    private String branchName;
}
