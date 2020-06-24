package io.visual_regression_tracker.sdk_java.request;

import lombok.Builder;

@Builder
public class TestRunRequest {
    String projectId;
    String buildId;
    String name;
    String imageBase64;
    String os;
    String browser;
    String viewport;
    String device;
    Integer diffTollerancePercent;
}
