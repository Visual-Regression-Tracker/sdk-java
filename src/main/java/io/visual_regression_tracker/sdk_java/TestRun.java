package io.visual_regression_tracker.sdk_java;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TestRun {
    String name;
    String imageBase64;
    String os;
    String browser;
    String viewport;
    String device;
    @Builder.Default
    Integer diffTollerancePercent = 1;
}
