package io.visual_regression_tracker.sdk_java.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BuildResponse {
    String id;
    String projectId;
}
