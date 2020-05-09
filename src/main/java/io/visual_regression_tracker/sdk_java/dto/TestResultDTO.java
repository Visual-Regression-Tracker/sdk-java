package io.visual_regression_tracker.sdk_java.dto;

import lombok.Getter;

@Getter
public
class TestResultDTO {
    String url;
    String status;
    Integer pixelMisMatchCount;
    Float diffPercent;
    Float diffTollerancePercent;
}
