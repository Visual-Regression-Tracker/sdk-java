package io.visual_regression_tracker.sdk_java.response;

import io.visual_regression_tracker.sdk_java.TestRunStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TestRunResponse {
    String url;
    TestRunStatus status;
    // not used for now
//    int pixelMisMatchCount;
//    float diffPercent;
//    float diffTollerancePercent;
}
