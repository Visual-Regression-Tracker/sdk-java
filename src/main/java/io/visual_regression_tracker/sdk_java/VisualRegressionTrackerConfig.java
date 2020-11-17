package io.visual_regression_tracker.sdk_java;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class VisualRegressionTrackerConfig {

    @NonNull
    private final String apiUrl;
    @NonNull
    private final String project;
    @NonNull
    private final String apiKey;
    @NonNull
    private final String branchName;
    @Builder.Default
    private Boolean enableSoftAssert = false;
    @Builder.Default
    private String ciBuildId = null;
}
