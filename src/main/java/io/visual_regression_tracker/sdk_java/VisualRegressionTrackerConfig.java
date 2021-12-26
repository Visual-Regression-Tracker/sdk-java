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
    private final String apiKey;
    @NonNull
    private final String project;
    @Builder.Default
    private String branchName = null;
    @Builder.Default
    private String ciBuildId = null;
    @Builder.Default
    private Boolean enableSoftAssert = false;
    @Builder.Default
    private int httpTimeoutInSeconds = 10;
}
