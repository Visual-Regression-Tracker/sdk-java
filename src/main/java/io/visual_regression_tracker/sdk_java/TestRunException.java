package io.visual_regression_tracker.sdk_java;

public class TestRunException extends RuntimeException {
    public TestRunException(String errorMessage) {
        super(errorMessage);
    }
}
