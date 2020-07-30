package io.visual_regression_tracker.sdk_java;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TestRunOptions {
    private final String os;
    private final String browser;
    private final String viewport;
    private final String device;
    private final Float diffTollerancePercent;
}
