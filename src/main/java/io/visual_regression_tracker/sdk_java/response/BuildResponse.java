package io.visual_regression_tracker.sdk_java.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildResponse {
    private final String id;
    private final String projectId;
    private final String ciBuildId;
    private final int number;
    private final String branchName;
    private final String status;
    private final String userId;
    private final int passedCount;
    private final int failedCount;
    private final int unresolvedCount;
    private final boolean merge;
    private final boolean isRunning;
}
