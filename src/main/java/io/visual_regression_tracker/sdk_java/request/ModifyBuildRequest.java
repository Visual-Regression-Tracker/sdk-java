package io.visual_regression_tracker.sdk_java.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModifyBuildRequest {
    private final Boolean isRunning;
    private final String ciBuildId;
}
