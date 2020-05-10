package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VisualRegressionTracker {
    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    Config config;
    String buildId;
    OkHttpClient client;

    public VisualRegressionTracker(Config config) {
        this.config = config;

        this.client = new OkHttpClient();
    }

    void startBuild() throws IOException {
        if (this.buildId == null) {
            Map<String, String> data = new HashMap<>();
            data.put("projectId", this.config.projectId);
            data.put("branchName", this.config.branchName);

            RequestBody body = RequestBody.create(new Gson().toJson(data), JSON);

            Request request = new Request.Builder()
                    .url(this.config.apiUrl.concat("/builds"))
                    .addHeader("apiKey", this.config.token)
                    .post(body)
                    .build();

            try (ResponseBody responseBody = client.newCall(request).execute().body()) {
                BuildDTO buildDTO = new Gson().fromJson(responseBody.string(), BuildDTO.class);
                this.buildId = buildDTO.getId();
            }
        }
    }

    TestResultDTO submitTestRun(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", this.config.projectId);
        data.put("buildId", this.buildId);
        data.put("name", name);
        data.put("imageBase64", imageBase64);
        data.put("os", testRunOptions.getOs());
        data.put("browser", testRunOptions.getBrowser());
        data.put("viewport", testRunOptions.getViewport());
        data.put("device", testRunOptions.getDevice());
        data.put("diffTollerancePercent", testRunOptions.getDiffTollerancePercent());

        RequestBody body = RequestBody.create(new Gson().toJson(data), JSON);

        Request request = new Request.Builder()
                .url(this.config.apiUrl.concat("/test"))
                .addHeader("apiKey", this.config.token)
                .post(body)
                .build();


        try (ResponseBody responseBody = client.newCall(request).execute().body()) {
            return new Gson().fromJson(responseBody.string(), TestResultDTO.class);
        }
    }

    public void track(String name, String imageBase64, TestRunOptions testRunOptions) throws IOException {
        this.startBuild();

        TestResultDTO testResultDTO = this.submitTestRun(name, imageBase64, testRunOptions);

        if (testResultDTO.getStatus().equals("new")) {
            throw new TestRunException("No baseline: ".concat(testResultDTO.getUrl()));
        }

        if (testResultDTO.getStatus().equals("unresolved")) {
            throw new TestRunException("Difference found: ".concat(testResultDTO.getUrl()));
        }
    }
}
