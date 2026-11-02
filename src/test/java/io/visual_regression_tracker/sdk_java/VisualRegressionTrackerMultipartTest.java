package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.expectThrows;

public class VisualRegressionTrackerMultipartTest {

    private static final String BUILD_ID = "buildId";
    private static final String PROJECT_ID = "projectId";
    private static final byte[] IMAGE = "PNG bytes".getBytes(StandardCharsets.UTF_8);

    private final Gson gson = new Gson();
    private MockWebServer mockWebServer;
    private VisualRegressionTrackerConfig config;
    private VisualRegressionTracker vrt;

    @SneakyThrows
    @BeforeMethod
    public void setup() {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        config = new VisualRegressionTrackerConfig(mockWebServer.url("/").toString(), "key", "project", "develop", null, false, 1);
        vrt = new VisualRegressionTracker(config);
        vrt.buildId = BUILD_ID;
        vrt.projectId = PROJECT_ID;
    }

    @SneakyThrows
    @AfterMethod
    public void tearDown() {
        mockWebServer.shutdown();
    }

    @Test
    public void trackBytesShouldPostEveryOptionAsMultipartForm() throws Exception {
        config.setSourceBranch("release");
        enqueueStatus(TestRunStatus.OK);

        vrt.track("Home page", IMAGE, TestRunOptions.builder()
                .os("Linux")
                .browser("Chrome")
                .viewport("800x600")
                .device("Desktop")
                .customTags("smoke")
                .comment("first run")
                .merge(true)
                .diffTollerancePercent(0.5f)
                .ignoreAreas(List.of(IgnoreAreas.builder().x(1L).y(2L).width(3L).height(4L).build()))
                .build());

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(request.getMethod(), is("POST"));
        assertThat(request.getPath(), endsWith("/test-runs/multipart"));
        assertThat(request.getHeader("Content-Type"), startsWith("multipart/form-data; boundary="));
        assertThat(request.getHeader(VisualRegressionTracker.API_KEY_HEADER), is("key"));
        assertThat(body, containsString(field("projectId", PROJECT_ID)));
        assertThat(body, containsString(field("buildId", BUILD_ID)));
        assertThat(body, containsString(field("name", "Home page")));
        assertThat(body, containsString(field("branchName", "develop")));
        assertThat(body, containsString(field("sourceBranch", "release")));
        assertThat(body, containsString(field("os", "Linux")));
        assertThat(body, containsString(field("browser", "Chrome")));
        assertThat(body, containsString(field("viewport", "800x600")));
        assertThat(body, containsString(field("device", "Desktop")));
        assertThat(body, containsString(field("customTags", "smoke")));
        assertThat(body, containsString(field("comment", "first run")));
        assertThat(body, containsString(field("merge", "true")));
        assertThat(body, containsString(field("diffTollerancePercent", "0.5")));
        assertThat(body, containsString(field("ignoreAreas", "[{\"x\":1,\"y\":2,\"width\":3,\"height\":4}]")));
        assertThat(body, containsString(image("image.png", "PNG bytes")));
        assertThat(body, not(containsString("imageBase64")));
    }

    @Test
    public void trackBytesShouldLeaveOutOptionsThatAreNotSet() throws Exception {
        enqueueStatus(TestRunStatus.OK);

        vrt.track("Home page", IMAGE);

        String body = mockWebServer.takeRequest().getBody().readUtf8();
        assertThat(body, not(containsString("name=\"os\"")));
        assertThat(body, not(containsString("name=\"comment\"")));
        assertThat(body, not(containsString("name=\"merge\"")));
        assertThat(body, not(containsString("name=\"sourceBranch\"")));
    }

    @Test
    public void trackPathShouldUploadTheFileUnderItsName() throws Exception {
        Path screenshot = Files.createTempDirectory("vrt").resolve("home.png");
        Files.write(screenshot, IMAGE);
        enqueueStatus(TestRunStatus.OK);

        vrt.track("Home page", screenshot);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath(), endsWith("/test-runs/multipart"));
        assertThat(request.getBody().readUtf8(), containsString(image("home.png", "PNG bytes")));
    }

    @Test
    public void trackBytesShouldFailOnDifferenceLikeBase64() {
        mockWebServer.enqueue(new MockResponse().setBody(gson.toJson(TestRunResponse.builder()
                .status(TestRunStatus.UNRESOLVED)
                .url("http://localhost:8080/test/1")
                .build())));

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.track("Home page", IMAGE));

        assertThat(exception.getMessage(), is("Difference found: http://localhost:8080/test/1"));
    }

    @Test
    public void payloadTooLargeOnMultipartShouldPointAtTheMultipartLimit() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(413).setBody("{\"message\":\"File too large\"}"));

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.track("Home page", IMAGE));

        assertThat(exception.getStatusCode(), is(413));
        assertThat(exception.getMessage(),
                is("Request failed with status 413: File too large. " + TestRunException.MULTIPART_TOO_LARGE_HINT));
    }

    @Test
    public void payloadTooLargeOnBase64ShouldSuggestMultipart() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(413).setBody("{\"message\":\"request entity too large\"}"));

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.track("Home page", "aW1hZ2U="));

        assertThat(exception.getMessage(),
                is("Request failed with status 413: request entity too large. " + TestRunException.PAYLOAD_TOO_LARGE_HINT));
    }

    private void enqueueStatus(TestRunStatus status) {
        mockWebServer.enqueue(new MockResponse().setBody(gson.toJson(TestRunResponse.builder().status(status).build())));
    }

    private static String field(String name, String value) {
        return "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
    }

    private static String image(String filename, String content) {
        return "Content-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n" + content + "\r\n";
    }
}
