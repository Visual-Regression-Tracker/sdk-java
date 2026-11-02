package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.ModifyBuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.ErrorResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

enum METHOD {
    GET,
    POST,
    PATCH
}

@Slf4j
public class VisualRegressionTracker {

    private static final String TRACKER_NOT_STARTED = "Visual Regression Tracker has not been started";
    private static final String CONFIG_FILE_NAME = "vrt.json";
    protected static final String API_KEY_HEADER = "apiKey";
    protected static final String PROJECT_HEADER = "project";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    static final int MAX_RETRIES = 3;
    static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(60);
    protected Gson gson;
    protected VisualRegressionTrackerConfig configuration;
    protected PathProvider paths;
    protected String buildId;
    protected String projectId;
    protected String branchName;
    protected final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public VisualRegressionTracker() {
        VisualRegressionTrackerConfig.VisualRegressionTrackerConfigBuilder configBuilder = VisualRegressionTrackerConfig.builder();
        File configFile = new File(CONFIG_FILE_NAME);
        if (configFile.exists()) {
            configBuilder.configFile(configFile);
        }
        configuration = configBuilder.build();
        paths = new PathProvider(configuration.getApiUrl());
        gson = new Gson();
    }

    public VisualRegressionTracker(VisualRegressionTrackerConfig trackerConfig) {
        configuration = trackerConfig;
        paths = new PathProvider(trackerConfig.getApiUrl());
        gson = new Gson();
    }

    public BuildResponse start() throws IOException, InterruptedException {
        String projectName = configuration.getProject();
        String branch = configuration.getBranchName();
        String ciBuildId = configuration.getCiBuildId();

        BuildRequest newBuild = BuildRequest.builder()
                .branchName(branch)
                .project(projectName)
                .ciBuildId(ciBuildId)
                .build();
        log.info("Starting Visual Regression Tracker for project <{}> and branch <{}>", projectName, branch);
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(gson.toJson(newBuild));
        HttpResponse<String> response = getResponse(METHOD.POST, paths.getBuildPath(), JSON_CONTENT_TYPE, body);
        BuildResponse buildResponse = handleResponse(response, BuildResponse.class);

        buildId = buildResponse.getId();
        projectId = buildResponse.getProjectId();
        branchName = buildResponse.getBranchName();

        log.info("Visual Regression Tracker is started for project <{}>: projectId: <{}>, buildId: <{}>, ciBuildId: <{}>",
                projectName, projectId, buildId, buildResponse.getCiBuildId());
        return buildResponse;
    }

    public BuildResponse stop() throws IOException, InterruptedException {
        if (!isStarted()) {
            throw new TestRunException(TRACKER_NOT_STARTED);
        }

        log.info("Stopping Visual Regression Tracker for buildId <{}>", buildId);

        ModifyBuildRequest stopBuildRequest = ModifyBuildRequest.builder()
                .isRunning(false)
                .build();
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(gson.toJson(stopBuildRequest));
        HttpResponse<String> response = getResponse(METHOD.PATCH, paths.getBuildPathForBuild(buildId), JSON_CONTENT_TYPE, body);
        BuildResponse vrtStopResponse = handleResponse(response, BuildResponse.class);

        log.info("Visual Regression Tracker is stopped for buildId <{}>", buildId);
        return vrtStopResponse;
    }

    public TestRunResult track(String name, String imageBase64, TestRunOptions testRunOptions)
            throws IOException, InterruptedException {
        log.info("Tracking test run <{}> with options <{}> for buildId <{}>", name, testRunOptions, buildId);
        return toResult(submitTestRun(name, imageBase64, testRunOptions));
    }

    public TestRunResult track(String name, String imageBase64) throws IOException, InterruptedException {
        return track(name, imageBase64, TestRunOptions.builder().build());
    }

    public TestRunResult track(String name, byte[] image, TestRunOptions testRunOptions)
            throws IOException, InterruptedException {
        return trackMultipart(name, image, "image.png", testRunOptions);
    }

    public TestRunResult track(String name, byte[] image) throws IOException, InterruptedException {
        return track(name, image, TestRunOptions.builder().build());
    }

    public TestRunResult track(String name, Path image, TestRunOptions testRunOptions)
            throws IOException, InterruptedException {
        return trackMultipart(name, Files.readAllBytes(image), image.getFileName().toString(), testRunOptions);
    }

    public TestRunResult track(String name, Path image) throws IOException, InterruptedException {
        return track(name, image, TestRunOptions.builder().build());
    }

    private TestRunResult trackMultipart(String name, byte[] image, String filename, TestRunOptions testRunOptions)
            throws IOException, InterruptedException {
        log.info("Tracking test run <{}> from {} with options <{}> for buildId <{}>", name, filename, testRunOptions, buildId);
        return toResult(submitTestRunMultipart(name, image, filename, testRunOptions));
    }

    private TestRunResult toResult(TestRunResponse testResultDTO) {
        String errorMessage;
        switch (testResultDTO.getStatus()) {
            case NEW:
                errorMessage = "No baseline: ".concat(testResultDTO.getUrl());
                break;
            case UNRESOLVED:
                errorMessage = "Difference found: ".concat(testResultDTO.getUrl());
                break;
            case UNKNOWN:
                log.warn("Unknown status of test run: {}", testResultDTO.getUrl());
                errorMessage = "";
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

    protected boolean isStarted() {
        return buildId != null && projectId != null;
    }

    protected TestRunResponse submitTestRun(String name, String imageBase64,
                                            TestRunOptions testRunOptions) throws IOException, InterruptedException {
        TestRunRequest newTestRun = testRunRequest(name, testRunOptions).imageBase64(imageBase64).build();

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(gson.toJson(newTestRun));
        HttpResponse<String> response = getResponse(METHOD.POST, paths.getTestRunPath(), JSON_CONTENT_TYPE, body);
        return handleResponse(response, TestRunResponse.class);
    }

    protected TestRunResponse submitTestRunMultipart(String name, byte[] image, String filename,
                                                     TestRunOptions testRunOptions) throws IOException, InterruptedException {
        JsonObject fields = gson.toJsonTree(testRunRequest(name, testRunOptions).build()).getAsJsonObject();
        String boundary = UUID.randomUUID().toString();

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary, fields, image, filename));
        HttpResponse<String> response = getResponse(METHOD.POST, paths.getTestRunMultipartPath(),
                "multipart/form-data; boundary=" + boundary, body);
        return handleResponse(response, TestRunResponse.class);
    }

    private TestRunRequest.TestRunRequestBuilder testRunRequest(String name, TestRunOptions testRunOptions) {
        if (!isStarted()) {
            throw new TestRunException(TRACKER_NOT_STARTED);
        }

        return TestRunRequest.builder()
                .projectId(projectId)
                .buildId(buildId)
                .branchName(configuration.getBranchName() != null ? configuration.getBranchName() : branchName)
                .sourceBranch(configuration.getSourceBranch())
                .name(name)
                .os(testRunOptions.getOs())
                .browser(testRunOptions.getBrowser())
                .viewport(testRunOptions.getViewport())
                .device(testRunOptions.getDevice())
                .customTags(testRunOptions.getCustomTags())
                .comment(testRunOptions.getComment())
                .merge(testRunOptions.getMerge())
                .diffTollerancePercent(testRunOptions.getDiffTollerancePercent())
                .ignoreAreas(testRunOptions.getIgnoreAreas());
    }

    private static byte[] multipartBody(String boundary, JsonObject fields, byte[] image, String filename) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Map.Entry<String, JsonElement> field : fields.entrySet()) {
            JsonElement value = field.getValue();
            writeUtf8(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
                    + (value.isJsonPrimitive() ? value.getAsString() : value.toString()) + "\r\n");
        }
        writeUtf8(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\""
                + filename.replace("\"", "%22") + "\"\r\nContent-Type: image/png\r\n\r\n");
        body.write(image);
        writeUtf8(body, "\r\n--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private static void writeUtf8(ByteArrayOutputStream stream, String text) throws IOException {
        stream.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> getResponse(METHOD method, String url, String contentType, HttpRequest.BodyPublisher body)
            throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .header(API_KEY_HEADER, configuration.getApiKey())
                .header(PROJECT_HEADER, configuration.getProject())
                .header("Content-Type", contentType)
                .uri(URI.create(url));
        if (configuration.getHttpTimeoutInSeconds() > 0) {
            requestBuilder.timeout(Duration.ofSeconds(configuration.getHttpTimeoutInSeconds()));
        }
        HttpRequest request = getRequest(method, body, requestBuilder);
        for (int retry = 1; ; retry++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Optional<Duration> wait = retryAfter(response);
            if (wait.isEmpty() || retry > MAX_RETRIES) {
                return response;
            }
            log.warn("Server is busy ({}), retry {} of {} in {} s", serverMessage(response.body()), retry, MAX_RETRIES,
                    wait.get().getSeconds());
            sleep(wait.get());
        }
    }

    private static Optional<Duration> retryAfter(HttpResponse<?> response) {
        if (response.statusCode() != 503) {
            return Optional.empty();
        }
        try {
            return response.headers().firstValue("Retry-After")
                    .map(Long::parseLong)
                    .filter(seconds -> seconds >= 0)
                    .map(seconds -> seconds > MAX_RETRY_AFTER.getSeconds() ? MAX_RETRY_AFTER : Duration.ofSeconds(seconds));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    protected void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    protected HttpRequest getRequest(METHOD method, HttpRequest.BodyPublisher body, HttpRequest.Builder requestBuilder) {
        switch (method) {
            case PATCH:
                return requestBuilder.method("PATCH", body).build();
            case POST:
                return requestBuilder.POST(body).build();
            default:
                throw new UnsupportedOperationException("This method is not yet supported.");
        }
    }

    protected <T> T handleResponse(HttpResponse<String> response, Class<T> classOfT) {
        String responseBody = response.body();
        if (!String.valueOf(response.statusCode()).startsWith("2")) {
            boolean multipart = response.uri().getPath().endsWith(PathProvider.MULTIPART_PATH);
            throw new TestRunException(response.statusCode(), serverMessage(responseBody), multipart);
        }
        return gson.fromJson(responseBody, classOfT);
    }

    private String serverMessage(String responseBody) {
        try {
            ErrorResponse error = gson.fromJson(responseBody, ErrorResponse.class);
            if (error != null && error.getMessage() != null) {
                return error.getMessage();
            }
        } catch (JsonSyntaxException ignored) {
            // not the server's JSON error body, e.g. an HTML page from a proxy
        }
        return responseBody;
    }
}
