package io.visual_regression_tracker.sdk_java.request;

import lombok.Builder;

@Builder
public class BuildRequest {
    String project;
    String branchName;
}
