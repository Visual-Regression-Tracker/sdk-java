package io.visual_regression_tracker.sdk_java;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VisualRegressionTrackerConfig {
    String apiUrl;
    String project;
    String apiKey;
    String branchName;
}
