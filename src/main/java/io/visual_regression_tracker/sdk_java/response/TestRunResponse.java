package io.visual_regression_tracker.sdk_java.response;

import io.visual_regression_tracker.sdk_java.TestRunStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TestRunResponse {
    private final String url;
    private final TestRunStatus status;
}
