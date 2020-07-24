package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import okhttp3.*;

import java.io.IOException;

public class VisualRegressionTracker {
    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    String apiKeyHeaderName = "apiKey";
    Gson gson = new Gson();
    VisualRegressionTrackerConfig visualRegressionTrackerConfig;
    String buildId;
    String projectId;
    OkHttpClient client;

    public VisualRegressionTracker(VisualRegressionTrackerConfig visualRegressionTrackerConfig) {
        this.visualRegressionTrackerConfig = visualRegressionTrackerConfig;

        this.client = new OkHttpClient();
    }

    void startBuild() throws IOException {
        if (this.buildId == null) {
            BuildRequest newBuild = BuildRequest.builder()
                    .branchName(this.visualRegressionTrackerConfig.branchName)
                    .project(this.visualRegressionTrackerConfig.project)
                    .build();

            RequestBody body = RequestBody.create(gson.toJson(newBuild), JSON);

            Request request = new Request.Builder()
                    .url(this.visualRegressionTrackerConfig.apiUrl.concat("/builds"))
                    .addHeader(apiKeyHeaderName, this.visualRegressionTrackerConfig.apiKey)
                    .post(body)
                    .build();

            try (ResponseBody responseBody = client.newCall(request).execute().body()) {
                BuildResponse buildDTO = new Gson().fromJson(responseBody.string(), BuildResponse.class);
                this.buildId = buildDTO.getId();
                this.projectId = buildDTO.getProjectId();
            } catch(Exception ex){
                System.out.println(ex.getMessage());
                throw ex;
            }
        }
    }

    TestRunResponse submitTestRun(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        TestRunRequest newTestRun = TestRunRequest.builder()
                .projectId(this.projectId)
                .buildId(this.buildId)
                .branchName(this.visualRegressionTrackerConfig.branchName)
                .name(name)
                .imageBase64(imageBase64)
                .os(testRunOptions.getOs())
                .browser(testRunOptions.getBrowser())
                .viewport(testRunOptions.getViewport())
                .device(testRunOptions.getDevice())
                .diffTollerancePercent(testRunOptions.getDiffTollerancePercent())
                .build();

        RequestBody body = RequestBody.create(gson.toJson(newTestRun), JSON);

        Request request = new Request.Builder()
                .url(this.visualRegressionTrackerConfig.apiUrl.concat("/test-runs"))
                .addHeader(apiKeyHeaderName, this.visualRegressionTrackerConfig.apiKey)
                .post(body)
                .build();

        try (ResponseBody responseBody = client.newCall(request).execute().body()) {
            return gson.fromJson(responseBody.string(), TestRunResponse.class);
        }
    }

    public void track(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        this.startBuild();

        TestRunResponse testResultDTO = this.submitTestRun(name, imageBase64, testRunOptions);

        if (testResultDTO.getStatus().equals(TestRunStatus.NEW)) {
            throw new TestRunException("No baseline: ".concat(testResultDTO.getUrl()));
        }

        if (testResultDTO.getStatus().equals(TestRunStatus.UNRESOLVED)) {
            throw new TestRunException("Difference found: ".concat(testResultDTO.getUrl()));
        }
    }

    public void track(String name, String imageBase64) throws IOException {
        this.track(name, imageBase64, TestRunOptions.builder().build());
    }
}
