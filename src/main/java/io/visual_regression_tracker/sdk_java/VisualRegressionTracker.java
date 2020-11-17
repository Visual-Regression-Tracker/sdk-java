package io.visual_regression_tracker.sdk_java;

import java.io.IOException;
import java.util.Optional;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class VisualRegressionTracker {

    private static final String TRACKER_NOT_STARTED = "Visual Regression Tracker has not been started";
    protected static final String API_KEY_HEADER = "apiKey";
    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    protected Gson gson;
    protected VisualRegressionTrackerConfig configuration;
    protected PathProvider paths;
    protected String buildId;
    protected String projectId;
    protected OkHttpClient client;

    public VisualRegressionTracker(VisualRegressionTrackerConfig trackerConfig) {
        configuration = trackerConfig;
        paths = new PathProvider(trackerConfig.getApiUrl());
        client = new OkHttpClient();
        gson = new Gson();
    }

    public void start() throws IOException {
        String projectName = configuration.getProject();
        String branch = configuration.getBranchName();

        BuildRequest newBuild = BuildRequest.builder()
                                            .branchName(branch)
                                            .project(projectName)
                                            .build();

        RequestBody body = RequestBody.create(JSON, gson.toJson(newBuild));

        Request request = new Request.Builder()
                                  .url(paths.getBuildPath())
                                  .addHeader(API_KEY_HEADER, configuration.getApiKey())
                                  .post(body)
                                  .build();

        log.info("Starting Visual Regression Tracker for project <{}> and branch <{}>", projectName, branch);

        Response response = client.newCall(request).execute();

        BuildResponse buildResponse = handleResponse(response, BuildResponse.class);

        buildId = buildResponse.getId();
        projectId = buildResponse.getProjectId();

        log.info("Visual Regression Tracker is started for project <{}>: buildId <{}>, projectId <{}>",
                 projectName, projectId, buildId);
    }

    public void stop() throws IOException {
        if (!isStarted()) {
            throw new TestRunException(TRACKER_NOT_STARTED);
        }

        Request request = new Request.Builder()
                                  .url(paths.getBuildPathForBuild(buildId))
                                  .addHeader(API_KEY_HEADER, configuration.getApiKey())
                                  .patch(RequestBody.create(JSON, ""))
                                  .build();

        log.info("Stopping Visual Regression Tracker for buildId <{}>", buildId);

        Response response = client.newCall(request).execute();
        handleResponse(response, Object.class);

        log.info("Visual Regression Tracker is stopped for buildId <{}>", buildId);
    }

    public TestRunResult track(String name, String imageBase64, TestRunOptions testRunOptions)
            throws IOException {
        log.info("Tracking test run <{}> with options <{}> for buildId <{}>", name, testRunOptions, buildId);
        TestRunResponse testResultDTO = submitTestRun(name, imageBase64, testRunOptions);

        String errorMessage;
        switch (testResultDTO.getStatus()) {
            case NEW:
                errorMessage = "No baseline: ".concat(testResultDTO.getUrl());
                break;
            case UNRESOLVED:
                errorMessage = "Difference found: ".concat(testResultDTO.getUrl());
                break;
            default:
                errorMessage = "";
                break;
        }

        if (!errorMessage.isEmpty()) {
            if (configuration.getEnableSoftAssert()) {
                log.error(errorMessage);
            } else {
                throw new TestRunException(errorMessage);
            }
        }

        return new TestRunResult(testResultDTO, this.paths);
    }

    public TestRunResult track(String name, String imageBase64) throws IOException {
        return track(name, imageBase64, TestRunOptions.builder().build());
    }

    protected boolean isStarted() {
        return buildId != null && projectId != null;
    }

    protected TestRunResponse submitTestRun(String name, String imageBase64,
                                            TestRunOptions testRunOptions) throws IOException {
        if (!isStarted()) {
            throw new TestRunException(TRACKER_NOT_STARTED);
        }

        TestRunRequest newTestRun = TestRunRequest.builder()
                                                  .projectId(projectId)
                                                  .buildId(buildId)
                                                  .branchName(configuration.getBranchName())
                                                  .name(name)
                                                  .imageBase64(imageBase64)
                                                  .os(testRunOptions.getOs())
                                                  .browser(testRunOptions.getBrowser())
                                                  .viewport(testRunOptions.getViewport())
                                                  .device(testRunOptions.getDevice())
                                                  .diffTollerancePercent(testRunOptions.getDiffTollerancePercent())
                                                  .ignoredAreas(testRunOptions.getIgnoredAreas())
                                                  .build();

        RequestBody body = RequestBody.create(JSON, gson.toJson(newTestRun));

        Request request = new Request.Builder()
                                  .url(paths.getTestRunPath())
                                  .addHeader(API_KEY_HEADER, configuration.getApiKey())
                                  .post(body)
                                  .build();

        Response response = client.newCall(request).execute();
        return handleResponse(response, TestRunResponse.class);
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
