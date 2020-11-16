package io.visual_regression_tracker.sdk_java.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildResponse {
    private final String id;
    private final String projectId;
    private final String ciBuildId;
}
