package io.visual_regression_tracker.sdk_java;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PathProvider {

    private static final String BUILD_PATH = "/builds";
    private static final String TEST_RUNS_PATH = "/test-runs";
    static final String MULTIPART_PATH = "/multipart";
    private static final String IMAGES_PATH = "/images/";

    private final String baseApiUrl;

    public String getBuildPath() {
        return baseApiUrl.concat(BUILD_PATH);
    }

    public String getBuildPathForBuild(String buildId) {
        return getBuildPath().concat("/").concat(buildId);
    }

    public String getTestRunPath() {
        return baseApiUrl.concat(TEST_RUNS_PATH);
    }

    public String getTestRunMultipartPath() {
        return getTestRunPath().concat(MULTIPART_PATH);
    }

    public String getImageUrl(String name) {
        if(name == null || name.isEmpty()){
            return null;
        }
        return baseApiUrl.concat(IMAGES_PATH).concat(name);
    }
}
