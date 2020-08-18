package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Objects;

public class VisualRegressionTrackerTest {
    private final Gson gson = new Gson();
    private final VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(
            "http://localhost",
            "733c148e-ef70-4e6d-9ae5-ab22263697cc",
            "XHGDZDFD3GMJDNM87JKEMP0JS1G5",
            "develop"
    );
    private MockWebServer server;
    private VisualRegressionTracker vrt;

    @SneakyThrows
    @BeforeMethod
    public void setup() {
        server = new MockWebServer();
        server.start();

        // target to mock server
        this.config.setApiUrl(server.url("/").toString());
        vrt = new VisualRegressionTracker(config);
    }

    @SneakyThrows
    @AfterMethod
    public void tearDown() {
        server.shutdown();
    }

    @DataProvider(name = "shouldReturnIsStartedCases")
    public Object[][] shouldReturnIsStartedCases() {
        return new Object[][]{
                {null, null, false},
                {null, "some", false},
                {"some", null, false},
                {"some", "some", true},
        };
    }

    @Test(dataProvider = "shouldReturnIsStartedCases")
    public void shouldReturnIsStarted(String buildId, String projectId, boolean expectedResult) {
        vrt.buildId = buildId;
        vrt.projectId = projectId;

        boolean result = vrt.isStarted();
        MatcherAssert.assertThat(result, CoreMatchers.is(expectedResult));
    }

    @Test
    public void shouldStartBuild() throws IOException, InterruptedException {
        String buildId = "123123";
        String projectId = "projectId";
        BuildRequest buildRequest = BuildRequest.builder()
                .branchName(this.config.getBranchName())
                .project(this.config.getProject())
                .build();
        BuildResponse buildResponse = BuildResponse.builder()
                .id(buildId)
                .projectId(projectId)
                .build();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        vrt.start();

        RecordedRequest request = server.takeRequest();
        MatcherAssert.assertThat(request.getHeader(vrt.apiKeyHeaderName), CoreMatchers.is(config.getApiKey()));
        MatcherAssert.assertThat(request.getBody().readUtf8(), CoreMatchers.is(gson.toJson(buildRequest)));
        MatcherAssert.assertThat(vrt.buildId, CoreMatchers.is(buildId));
        MatcherAssert.assertThat(vrt.projectId, CoreMatchers.is(projectId));
    }

    @Test
    public void shouldThrowExceptionIfProjectNotFound() throws IOException {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\r\n  \"statusCode\": 404,\r\n  \"message\": \"Project not found\"\r\n}"));

        String exceptionMessage = "";
        try {
            vrt.start();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Project not found"));
    }

    @Test
    public void shouldThrowExceptionIfUnauthorized() throws IOException {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\r\n  \"statusCode\": 401,\r\n  \"message\": \"Unauthorized\"\r\n}"));

        String exceptionMessage = "";
        try {
            vrt.start();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Unauthorized"));
    }

    @Test
    public void shouldThrowExceptionIfForbidden() throws IOException {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\r\n  \"statusCode\": 403,\r\n  \"message\": \"Forbidden\"\r\n}"));

        String exceptionMessage = "";
        try {
            vrt.start();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Api key not authenticated"));
    }

    @Test
    public void shouldStopBuild() throws IOException, InterruptedException {
        String buildId = "123123";
        String projectId = " someId";
        vrt.buildId = buildId;
        vrt.projectId = projectId;
        BuildResponse buildResponse = BuildResponse.builder()
                .id(buildId)
                .build();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        vrt.stop();

        RecordedRequest request = server.takeRequest();
        MatcherAssert.assertThat(request.getMethod(), CoreMatchers.is("PATCH"));
        MatcherAssert.assertThat(request.getHeader(vrt.apiKeyHeaderName), CoreMatchers.is(config.getApiKey()));
        MatcherAssert.assertThat(Objects.requireNonNull(request.getRequestUrl()).encodedPath(), CoreMatchers.containsString(buildId));
    }

    @Test
    public void stopShouldThrowExceptionIfNotStarted() throws IOException {
        String exceptionMessage = "";
        try {
            vrt.stop();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Visual Regression Tracker has not been started"));
    }

    @Test
    public void shouldSubmitTestRun() throws IOException, InterruptedException {
        String buildId = "123123";
        String projectId = "projectId";
        String name = "Test name";
        String imageBase64 = "image";
        TestRunOptions testRunOptions = TestRunOptions.builder()
                .device("Device")
                .os("OS")
                .browser("Browser")
                .viewport("Viewport")
                .diffTollerancePercent(0.5f)
                .build();
        TestRunRequest testRunRequest = TestRunRequest.builder()
                .projectId(projectId)
                .branchName(config.getBranchName())
                .buildId(buildId)
                .name(name)
                .imageBase64(imageBase64)
                .os(testRunOptions.getOs())
                .browser(testRunOptions.getBrowser())
                .viewport(testRunOptions.getViewport())
                .device(testRunOptions.getDevice())
                .diffTollerancePercent(testRunOptions.getDiffTollerancePercent())
                .build();
        TestRunResponse testRunResponse = TestRunResponse.builder()
                .status(TestRunStatus.UNRESOLVED)
                .build();
        server.enqueue(new MockResponse().setBody(gson.toJson(testRunResponse)));
        vrt.buildId = buildId;
        vrt.projectId = projectId;

        TestRunResponse result = vrt.submitTestRun(name, imageBase64, testRunOptions);

        RecordedRequest request = server.takeRequest();
        MatcherAssert.assertThat(request.getHeader(vrt.apiKeyHeaderName), CoreMatchers.is(config.getApiKey()));
        MatcherAssert.assertThat(request.getBody().readUtf8(), CoreMatchers.is(gson.toJson(testRunRequest)));
        MatcherAssert.assertThat(gson.toJson(result), CoreMatchers.is(gson.toJson(testRunResponse)));
    }

    @Test
    public void shouldNotSubmitTestRunIfNotStarted() throws IOException {
        VisualRegressionTracker vrtMocked = Mockito.mock(VisualRegressionTracker.class);
        Mockito.when(vrtMocked.isStarted()).thenReturn(false);

        Mockito.doCallRealMethod().when(vrtMocked).submitTestRun(Mockito.anyString(), Mockito.any(), Mockito.any());
        String exceptionMessage = "";
        try {
            vrtMocked.submitTestRun("name", null, null);
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Visual Regression Tracker has not been started"));
    }

    @DataProvider(name = "shouldTrackThrowExceptionCases")
    public Object[][] shouldTrackThrowExceptionCases() {
        return new Object[][]{
                {
                        TestRunResponse.builder()
                                .url("https://someurl.com/test/123123")
                                .status(TestRunStatus.UNRESOLVED)
                                .build(),
                        "Difference found: https://someurl.com/test/123123"
                },
                {
                        TestRunResponse.builder()
                                .url("https://someurl.com/test/123123")
                                .status(TestRunStatus.NEW)
                                .build(),
                        "No baseline: https://someurl.com/test/123123"
                }
        };
    }

    @Test(dataProvider = "shouldTrackThrowExceptionCases")
    public void shouldTrackThrowException(TestRunResponse testRunResponse, String expectedExceptionMessage) throws IOException {
        VisualRegressionTracker vrtMocked = Mockito.mock(VisualRegressionTracker.class);
        Mockito.when(vrtMocked.submitTestRun(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(testRunResponse);

        Mockito.doCallRealMethod().when(vrtMocked).track(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        String exceptionMessage = "";
        try {
            vrtMocked.track("name", "image", TestRunOptions.builder().build());
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is(expectedExceptionMessage));
    }

    @DataProvider(name = "shouldTrackPassCases")
    public Object[][] shouldTrackPassCases() {
        return new Object[][]{
                {
                        TestRunResponse.builder()
                                .url("https://someurl.com/test/123123")
                                .status(TestRunStatus.OK)
                                .build(),
                }
        };
    }

    @Test(dataProvider = "shouldTrackPassCases")
    public void shouldTrackPass(TestRunResponse testRunResponse) throws IOException {
        VisualRegressionTracker vrtMocked = Mockito.mock(VisualRegressionTracker.class);
        Mockito.when(vrtMocked.submitTestRun(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(testRunResponse);

        Mockito.doCallRealMethod().when(vrtMocked).track(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        vrtMocked.track("name", "image", TestRunOptions.builder().build());
    }

    @Test()
    public void shouldTrackOverload() throws IOException {
        VisualRegressionTracker vrtMocked = Mockito.mock(VisualRegressionTracker.class);

        Mockito.doCallRealMethod().when(vrtMocked).track(Mockito.anyString(), Mockito.anyString());
        vrtMocked.track("name", "image");

        Mockito.verify(vrtMocked, Mockito.times(1)).track(Mockito.anyString(), Mockito.anyString(), Mockito.any(TestRunOptions.class));
    }
}
