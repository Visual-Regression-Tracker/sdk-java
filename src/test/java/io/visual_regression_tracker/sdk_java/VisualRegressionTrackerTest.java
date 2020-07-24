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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

public class VisualRegressionTrackerTest {
    Gson gson = new Gson();
    MockWebServer server;
    VisualRegressionTracker vrt;
    VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(
            "http://localhost",
            "733c148e-ef70-4e6d-9ae5-ab22263697cc",
            "XHGDZDFD3GMJDNM87JKEMP0JS1G5",
            "develop"
    );

    @SneakyThrows
    @BeforeTest
    void setup() {
        server = new MockWebServer();
        server.start();

        // target to mock server
        this.config.apiUrl = server.url("/").toString();
        vrt = new VisualRegressionTracker(config);
    }

    @SneakyThrows
    @AfterTest
    void tearDown() {
        server.shutdown();
    }

    @Test
    void shouldStartBuild() throws IOException, InterruptedException {
        String buildId = "123123";
        String projectId = "projectId";
        BuildRequest buildRequest = BuildRequest.builder()
                .branchName(this.config.branchName)
                .project(this.config.project)
                .build();
        BuildResponse buildResponse = BuildResponse.builder()
                .id(buildId)
                .projectId(projectId)
                .build();
        server.enqueue(new MockResponse().setBody(gson.toJson(buildResponse)));

        vrt.startBuild();

        RecordedRequest request = server.takeRequest();
        MatcherAssert.assertThat(request.getHeader(vrt.apiKeyHeaderName), CoreMatchers.is(config.apiKey));
        MatcherAssert.assertThat(request.getBody().readUtf8(), CoreMatchers.is(gson.toJson(buildRequest)));
        MatcherAssert.assertThat(vrt.buildId, CoreMatchers.is(buildId));
        MatcherAssert.assertThat(vrt.projectId, CoreMatchers.is(projectId));
    }

    @Test
    void shouldThrowExceptionIfProjectNotFound() throws IOException {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\r\n  \"statusCode\": 404,\r\n  \"message\": \"Project not found\"\r\n}"));

        String exceptionMessage = "";
        try {
            vrt.startBuild();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Project not found"));
    }

    @Test
    void shouldThrowExceptionIfUnauthorized() throws IOException {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\r\n  \"statusCode\": 401,\r\n  \"message\": \"Unauthorized\"\r\n}"));

        String exceptionMessage = "";
        try {
            vrt.startBuild();
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }
        MatcherAssert.assertThat(exceptionMessage, CoreMatchers.is("Unauthorized"));
    }

    @Test
    void shouldSubmitTestRun() throws IOException, InterruptedException {
        String buildId = "123123";
        String projectId = "projectId";
        String name = "Test name";
        String imageBase64 = "image";
        TestRunOptions testRunOptions = TestRunOptions.builder()
                .device("Device")
                .os("OS")
                .browser("Browser")
                .viewport("Viewport")
                .diffTollerancePercent(5)
                .build();
        TestRunRequest testRunRequest = TestRunRequest.builder()
                .projectId(projectId)
                .branchName(config.branchName)
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

        TestRunResponse result = vrt.submitTestRun(name, imageBase64, testRunOptions);

        RecordedRequest request = server.takeRequest();
        MatcherAssert.assertThat(request.getHeader(vrt.apiKeyHeaderName), CoreMatchers.is(config.apiKey));
        MatcherAssert.assertThat(request.getBody().readUtf8(), CoreMatchers.is(gson.toJson(testRunRequest)));
        MatcherAssert.assertThat(gson.toJson(result), CoreMatchers.is(gson.toJson(testRunResponse)));
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
