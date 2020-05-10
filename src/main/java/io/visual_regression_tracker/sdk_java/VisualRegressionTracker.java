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

    TestResultDTO submitTestRun(TestRun testRun) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", this.config.projectId);
        data.put("buildId", this.buildId);
        data.put("name", testRun.getName());
        data.put("imageBase64", testRun.getImageBase64());
        data.put("os", testRun.getOs());
        data.put("browser", testRun.getBrowser());
        data.put("viewport", testRun.getViewport());
        data.put("device", testRun.getDevice());
        data.put("diffTollerancePercent", testRun.getDiffTollerancePercent());

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

    public void track(TestRun testRun) throws IOException {
        this.startBuild();

        TestResultDTO testResultDTO = this.submitTestRun(testRun);

        if (testResultDTO.getStatus().equals("new")) {
            throw new TestRunException("No baseline: ".concat(testResultDTO.getUrl()));
        }

        if (testResultDTO.getStatus().equals("unresolved")) {
            throw new TestRunException("Difference found: ".concat(testResultDTO.getUrl()));
        }
    }
}
