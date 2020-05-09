package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.dto.BuildDTO;
import io.visual_regression_tracker.sdk_java.dto.TestResultDTO;
import io.visual_regression_tracker.sdk_java.dto.TestRun;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class VisualRegressionTracker {
    Config config;
    String buildId;
    HttpClient client;

    public VisualRegressionTracker(Config config) {
        this.config = config;

        this.client = HttpClient.newHttpClient();
    }

    void startBuild() throws IOException, InterruptedException {
        if (this.buildId == null) {
            Map<String, String> data = new HashMap<>();
            data.put("projectId", this.config.projectId);
            data.put("branchName", this.config.branchName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.config.apiUrl.concat("/builds")))
                    .header("apiKey", this.config.token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(data)))
                    .build();

            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            BuildDTO buildDTO = new Gson().fromJson(response.body(), BuildDTO.class);

            this.buildId = buildDTO.getId();
        }
    }

    TestResultDTO submitTestRun(TestRun testRun) throws IOException, InterruptedException {
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


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.config.apiUrl.concat("/test")))
                .header("apiKey", this.config.token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(data)))
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
        return new Gson().fromJson(response.body(), TestResultDTO.class);
    }

    public void track(TestRun testRun) throws IOException, InterruptedException {
        this.startBuild();

        TestResultDTO testResultDTO = this.submitTestRun(testRun);

        if(testResultDTO.getStatus().equals("new")) {
            throw new TestRunException("No baseline: ".concat(testResultDTO.getUrl()));
        }

        if(testResultDTO.getStatus().equals("unresolved")) {
            throw new TestRunException("Difference found: ".concat(testResultDTO.getUrl()));
        }
    }
}
