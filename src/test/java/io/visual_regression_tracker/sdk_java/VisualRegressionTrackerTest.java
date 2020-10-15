package io.visual_regression_tracker.sdk_java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.request.BuildRequest;
import io.visual_regression_tracker.sdk_java.request.TestRunRequest;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import io.visual_regression_tracker.sdk_java.response.TestRunResponse;
import lombok.SneakyThrows;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class VisualRegressionTrackerTest {

    private final static String BUILD_ID = "123123";
    private final static String PROJECT_ID = "projectId";
    private final static String NAME = "Test name";
    private final static String IMAGE_BASE_64 = "image";

    private final Gson gson = new Gson();
    private final VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(
            "http://localhost",
            "733c148e-ef70-4e6d-9ae5-ab22263697cc",
            "XHGDZDFD3GMJDNM87JKEMP0JS1G5",
            "develop",
            false
    );

    private MockWebServer server;
    private VisualRegressionTracker vrt;
    private VisualRegressionTracker vrtMocked;

    @SneakyThrows
    @BeforeMethod
    public void setup() {
        server = new MockWebServer();
        server.start();

        // target to mock server
        this.config.setApiUrl(server.url("/").toString());
        vrt = new VisualRegressionTracker(config);
        vrtMocked = mock(VisualRegressionTracker.class);
        vrtMocked.paths = new PathProvider("baseApiUrl");
    }

    @SneakyThrows
    @AfterMethod
    public void tearDown() {
        server.shutdown();
        reset(vrtMocked);
    }

    @Test
    public void shouldStartBuild() throws IOException, InterruptedException {
        BuildRequest buildRequest = BuildRequest.builder()
                .branchName(this.config.getBranchName())
                .project(this.config.getProject())
                .build();
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .projectId(PROJECT_ID)
                .build();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        vrt.start();

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(recordedRequest.getBody().readUtf8(), is(gson.toJson(buildRequest)));
        assertThat(vrt.buildId, is(BUILD_ID));
        assertThat(vrt.projectId, is(PROJECT_ID));
    }

    @Test
    public void shouldStopBuild() throws IOException, InterruptedException {
        vrt.buildId = BUILD_ID;
        vrt.projectId = PROJECT_ID;
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .build();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        vrt.stop();

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getMethod(), is("PATCH"));
        assertThat(recordedRequest.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(Objects.requireNonNull(recordedRequest.getRequestUrl()).encodedPath(), containsString(BUILD_ID));
    }

    @Test(expectedExceptions = TestRunException.class,
            expectedExceptionsMessageRegExp = "Visual Regression Tracker has not been started")
    public void stopShouldThrowExceptionIfNotStarted() throws IOException {
        vrt.stop();
    }

    @Test
    public void shouldSubmitTestRun() throws IOException, InterruptedException {
        TestRunOptions testRunOptions = TestRunOptions.builder()
                .device("Device")
                .os("OS")
                .browser("Browser")
                .viewport("Viewport")
                .diffTollerancePercent(0.5f)
                .build();
        TestRunRequest testRunRequest = TestRunRequest.builder()
                .projectId(PROJECT_ID)
                .branchName(config.getBranchName())
                .buildId(BUILD_ID)
                .name(NAME)
                .imageBase64(IMAGE_BASE_64)
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
        vrt.buildId = BUILD_ID;
        vrt.projectId = PROJECT_ID;

        TestRunResponse result = vrt.submitTestRun(NAME, IMAGE_BASE_64, testRunOptions);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(request.getBody().readUtf8(), is(gson.toJson(testRunRequest)));
        assertThat(gson.toJson(result), is(gson.toJson(testRunResponse)));
    }

    @Test(expectedExceptions = TestRunException.class,
            expectedExceptionsMessageRegExp = "Visual Regression Tracker has not been started")
    public void submitTestRunShouldThrowIfNotStarted() throws IOException {
        when(vrtMocked.isStarted()).thenReturn(false);

        doCallRealMethod().when(vrtMocked).submitTestRun(anyString(), any(), any());
        vrtMocked.submitTestRun("name", null, null);
    }

    @DataProvider(name = "trackErrorCases")
    public Object[][] trackErrorCases() {
        return new Object[][]{
                {
                        TestRunResponse.builder()
                                .url("https://someurl.com/test/123123")
                                .imageName("imageName")
                                .baselineName("baselineName")
                                .diffName("diffName")
                                .status(TestRunStatus.UNRESOLVED)
                                .build(),
                        "Difference found: https://someurl.com/test/123123"
                },
                {
                        TestRunResponse.builder()
                                .url("https://someurl.com/test/123123")
                                .imageName("imageName")
                                .baselineName("baselineName")
                                .diffName("diffName")
                                .status(TestRunStatus.NEW)
                                .build(),
                        "No baseline: https://someurl.com/test/123123"
                }
        };
    }

    @Test(dataProvider = "trackErrorCases",
            expectedExceptions = TestRunException.class,
            expectedExceptionsMessageRegExp = "^(Difference found: https://someurl.com/test/123123|No baseline: https://someurl.com/test/123123)$")
    public void trackShouldThrowException(TestRunResponse testRunResponse, String expectedExceptionMessage) throws IOException {
        vrtMocked.configuration = new VisualRegressionTrackerConfig("", "", "", "", false);
        when(vrtMocked.submitTestRun(anyString(), anyString(), any())).thenReturn(testRunResponse);

        doCallRealMethod().when(vrtMocked).track(anyString(), anyString(), any());
        vrtMocked.track("name", "image", TestRunOptions.builder().build());
    }

    @Test(dataProvider = "trackErrorCases")
    public void trackShouldLogSevere(TestRunResponse testRunResponse, String expectedExceptionMessage) throws IOException {
        Logger loggerMock = (Logger) LoggerFactory.getLogger(VisualRegressionTracker.class);
        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        // add the appender to the logger
        loggerMock.addAppender(listAppender);
        vrtMocked.configuration = new VisualRegressionTrackerConfig("", "", "", "", true);
        when(vrtMocked.submitTestRun(anyString(), anyString(), any())).thenReturn(testRunResponse);

        doCallRealMethod().when(vrtMocked).track(anyString(), anyString(), any());
        vrtMocked.track("name", "image", TestRunOptions.builder().build());

        assertThat(listAppender.list.get(1).getFormattedMessage(), is(expectedExceptionMessage));
        assertThat(listAppender.list.get(1).getLevel(), is(Level.ERROR));
    }

    @DataProvider(name = "shouldTrackPassCases")
    public Object[][] shouldTrackPassCases() {
        return new Object[][]{
                {
                        TestRunResponse.builder()
                                .id("someId")
                                .imageName("imageName")
                                .baselineName("baselineName")
                                .diffName("diffName")
                                .diffPercent(12.32f)
                                .diffTollerancePercent(0.01f)
                                .pixelMisMatchCount(1)
                                .merge(false)
                                .url("https://someurl.com/test/123123")
                                .status(TestRunStatus.OK)
                                .build(),
                }
        };
    }

    @Test(dataProvider = "shouldTrackPassCases")
    public void shouldTrackPass(TestRunResponse testRunResponse) throws IOException {
        when(vrtMocked.submitTestRun(anyString(), anyString(), any())).thenReturn(testRunResponse);
        vrtMocked.paths = new PathProvider("backendUrl");

        doCallRealMethod().when(vrtMocked).track(anyString(), anyString(), any());
        TestRunResult testRunResult = vrtMocked.track("name", "image", TestRunOptions.builder().build());

        assertThat(testRunResult.getTestRunResponse(), is(testRunResponse));
        assertThat(testRunResult.getImageUrl(), is("backendUrl/".concat(testRunResponse.getImageName())));
        assertThat(testRunResult.getDiffUrl(), is("backendUrl/".concat(testRunResponse.getDiffName())));
        assertThat(testRunResult.getBaselineUrl(), is("backendUrl/".concat(testRunResponse.getBaselineName())));
    }

    @Test()
    public void shouldTrackOverload() throws IOException {
        doCallRealMethod().when(vrtMocked).track(anyString(), anyString());
        vrtMocked.track("name", "image");

        verify(vrtMocked, times(1)).track(anyString(), anyString(), any(TestRunOptions.class));
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

        assertThat(result, is(expectedResult));
    }

    @Test
    public void handleRequestShouldThrowIfNotSuccess() throws IOException {
        String error = "{\n" +
                "  \"statusCode\": 404,\n" +
                "  \"message\": \"Project not found\"\n" +
                "}";
        Request mockRequest = new Request.Builder()
                .url(config.getApiUrl())
                .build();

        String exceptionMessage = "";
        try {
            vrt.handleResponse(new Response.Builder()
                    .request(mockRequest)
                    .protocol(Protocol.HTTP_2)
                    .code(404)
                    .message("Not found")
                    .body(ResponseBody.create(error, VisualRegressionTracker.JSON))
                    .build(), Object.class);
        } catch (TestRunException ex) {
            exceptionMessage = ex.getMessage();
        }

        assertThat(exceptionMessage, is(error));
    }
}
