package io.visual_regression_tracker.sdk_java;

import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.Getter;

@Getter
public class TestRunResult {
  private final TestRunResponse testRunResponse;
  private final String imageUrl;
  private final String diffUrl;
  private final String baselineUrl;

  public TestRunResult(TestRunResponse testRunResponse, PathProvider pathProvider) {
    this.testRunResponse = testRunResponse;
    this.imageUrl = pathProvider.getImageUrl(testRunResponse.getImageName());
    this.diffUrl = pathProvider.getImageUrl(testRunResponse.getDiffName());
    this.baselineUrl = pathProvider.getImageUrl(testRunResponse.getBaselineName());
  }
}
