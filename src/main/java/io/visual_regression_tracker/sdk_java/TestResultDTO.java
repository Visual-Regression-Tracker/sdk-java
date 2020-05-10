package io.visual_regression_tracker.sdk_java;

import lombok.Getter;

@Getter
class TestResultDTO {
    String url;
    String status;
    Integer pixelMisMatchCount;
    Float diffPercent;
    Float diffTollerancePercent;
}
