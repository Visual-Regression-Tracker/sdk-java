package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import okhttp3.*;

import java.io.IOException;
import java.util.Optional;

public class VisualRegressionTracker {
    protected static final String apiKeyHeaderName = "apiKey";
    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    protected Gson gson;
    protected VisualRegressionTrackerConfig visualRegressionTrackerConfig;
    protected String buildId;
    protected String projectId;
    protected OkHttpClient client;

    public VisualRegressionTracker(VisualRegressionTrackerConfig visualRegressionTrackerConfig) {
        this.visualRegressionTrackerConfig = visualRegressionTrackerConfig;

        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void start() throws IOException {
        BuildRequest newBuild = BuildRequest.builder()
                .branchName(this.visualRegressionTrackerConfig.getBranchName())
                .project(this.visualRegressionTrackerConfig.getProject())
                .build();

        RequestBody body = RequestBody.create(JSON, gson.toJson(newBuild));

        Request request = new Request.Builder()
                .url(this.visualRegressionTrackerConfig.getApiUrl().concat("/builds"))
                .addHeader(apiKeyHeaderName, this.visualRegressionTrackerConfig.getApiKey())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {

            BuildResponse buildDTO = handleResponse(response, BuildResponse.class);

            this.buildId = Optional.ofNullable(buildDTO.getId())
                    .orElseThrow(() -> new TestRunException("Build id is null"));
            this.projectId = Optional.ofNullable(buildDTO.getProjectId())
                    .orElseThrow(() -> new TestRunException("Project id is null"));
        }
    }

    public void stop() throws IOException {
        if (!this.isStarted()) {
            throw new TestRunException("Visual Regression Tracker has not been started");
        }

        Request request = new Request.Builder()
                .url(this.visualRegressionTrackerConfig.getApiUrl().concat("/builds/").concat(this.buildId))
                .addHeader(apiKeyHeaderName, this.visualRegressionTrackerConfig.getApiKey())
                .patch(RequestBody.create(JSON, ""))
                .build();

        try (Response response = client.newCall(request).execute()) {
            handleResponse(response, Object.class);
        }
    }

    public void track(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        TestRunResponse testResultDTO = this.submitTestRun(name, imageBase64, testRunOptions);

        TestRunStatus status = Optional.ofNullable(testResultDTO.getStatus())
                .orElseThrow(() -> new TestRunException("Status is null"));

        if (status.equals(TestRunStatus.NEW)) {
            throw new TestRunException("No baseline: ".concat(testResultDTO.getUrl()));
        }

        if (status.equals(TestRunStatus.UNRESOLVED)) {
            throw new TestRunException("Difference found: ".concat(testResultDTO.getUrl()));
        }
    }

    public void track(String name, String imageBase64) throws IOException {
        this.track(name, imageBase64, TestRunOptions.builder().build());
    }

    protected boolean isStarted() {
        return this.buildId != null && this.projectId != null;
    }

    protected TestRunResponse submitTestRun(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        if (!this.isStarted()) {
            throw new TestRunException("Visual Regression Tracker has not been started");
        }

        TestRunRequest newTestRun = TestRunRequest.builder()
                .projectId(this.projectId)
                .buildId(this.buildId)
                .branchName(this.visualRegressionTrackerConfig.getBranchName())
                .name(name)
                .imageBase64(imageBase64)
                .os(testRunOptions.getOs())
                .browser(testRunOptions.getBrowser())
                .viewport(testRunOptions.getViewport())
                .device(testRunOptions.getDevice())
                .diffTollerancePercent(testRunOptions.getDiffTollerancePercent())
                .build();

        RequestBody body = RequestBody.create(JSON, gson.toJson(newTestRun));

        Request request = new Request.Builder()
                .url(this.visualRegressionTrackerConfig.getApiUrl().concat("/test-runs"))
                .addHeader(apiKeyHeaderName, this.visualRegressionTrackerConfig.getApiKey())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, TestRunResponse.class);
        }
    }

    protected <T> T handleResponse(Response response, Class<T> classOfT) throws IOException {
        String responseBody = Optional.ofNullable(response.body())
                .orElseThrow(() -> new TestRunException("Cannot get response body"))
                .string();

        if (!response.isSuccessful()) {
            throw new TestRunException(responseBody);
        }

        return gson.fromJson(responseBody, classOfT);
    }
}
