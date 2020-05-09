package io.visual_regression_tracker.sdk_java;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Config {
    String apiUrl;
    String branchName;
    String projectId;
    String token;
}
