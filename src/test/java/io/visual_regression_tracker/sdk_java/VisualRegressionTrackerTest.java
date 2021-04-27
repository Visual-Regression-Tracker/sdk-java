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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VisualRegressionTrackerTest {

    private final static String BUILD_ID = "123123";
    private final static String PROJECT_ID = "projectId";
    private final static String CI_BUILD_ID = "123456789";
    private final static String NAME = "Test name";
    private final static String IMAGE_BASE_64 = "image";
    private final static int HTTP_TIMEOUT = 1;

    private final Gson gson = new Gson();

    private VisualRegressionTrackerConfig config;
    private MockWebServer mockWebServer;
    private VisualRegressionTracker vrt;
    private VisualRegressionTracker vrtMocked;

    @SneakyThrows
    @BeforeMethod
    public void setup() {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // target to mock server
        config = new VisualRegressionTrackerConfig(
                mockWebServer.url("/").toString(),
                "733c148e-ef70-4e6d-9ae5-ab22263697cc",
                "XHGDZDFD3GMJDNM87JKEMP0JS1G5",
                "develop",
                false,
                CI_BUILD_ID,
                HTTP_TIMEOUT);
        vrt = new VisualRegressionTracker(config);
        vrtMocked = mock(VisualRegressionTracker.class);
        vrtMocked.paths = new PathProvider("baseApiUrl");
    }

    @SneakyThrows
    @AfterMethod
    public void tearDown() {
        mockWebServer.shutdown();
        reset(vrtMocked);
    }

    @Test
    public void shouldStartBuild() throws IOException, InterruptedException {
        BuildRequest buildRequest = BuildRequest.builder()
                .branchName(this.config.getBranchName())
                .project(this.config.getProject())
                .ciBuildId(this.config.getCiBuildId())
                .build();
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .projectId(PROJECT_ID)
                .ciBuildId(CI_BUILD_ID)
                .build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        BuildResponse result = vrt.start();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(recordedRequest.getBody().readUtf8(), is(gson.toJson(buildRequest)));
        assertThat(vrt.buildId, is(BUILD_ID));
        assertThat(vrt.projectId, is(PROJECT_ID));
        assertThat(result.getId(), is(BUILD_ID));
        assertThat(result.getProjectId(), is(PROJECT_ID));
        assertThat(result.getCiBuildId(), is(CI_BUILD_ID));
    }

    @Test
    public void shouldStopBuild() throws IOException, InterruptedException {
        vrt.buildId = BUILD_ID;
        vrt.projectId = PROJECT_ID;
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .passedCount(1)
                .unresolvedCount(2)
                .isRunning(false)
                .status("unresolved")
                .build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(buildResponse)));

        BuildResponse actualBuildResponse = vrt.stop();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("PATCH"));
        assertThat(recordedRequest.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(Objects.requireNonNull(recordedRequest.getRequestUrl()).encodedPath(), containsString(BUILD_ID));
        assertThat(actualBuildResponse.isRunning(), is(false));
        assertThat(actualBuildResponse.getStatus(), containsString("unresolved"));
        assertThat(actualBuildResponse.getPassedCount(), is(1));
        assertThat(actualBuildResponse.getUnresolvedCount(), is(2));
    }

    @Test(expectedExceptions = TestRunException.class,
            expectedExceptionsMessageRegExp = "Visual Regression Tracker has not been started")
    public void stopShouldThrowExceptionIfNotStarted() throws IOException, InterruptedException {
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
                .ignoreAreas(Arrays.asList(IgnoreAreas.builder()
                        .x(100L)
                        .y(100L)
                        .height(1L)
                        .width(1L)
                        .build()))
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
                .ignoreAreas(testRunOptions.getIgnoreAreas())
                .build();
        TestRunResponse testRunResponse = TestRunResponse.builder()
                .status(TestRunStatus.UNRESOLVED)
                .build();
        mockWebServer.enqueue(new MockResponse().setBody(gson.toJson(testRunResponse)));
        vrt.buildId = BUILD_ID;
        vrt.projectId = PROJECT_ID;

        TestRunResponse result = vrt.submitTestRun(NAME, IMAGE_BASE_64, testRunOptions);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader(VisualRegressionTracker.API_KEY_HEADER), is(config.getApiKey()));
        assertThat(request.getBody().readUtf8(), is(gson.toJson(testRunRequest)));
        assertThat(gson.toJson(result), is(gson.toJson(testRunResponse)));
    }

    @Test(expectedExceptions = TestRunException.class,
            expectedExceptionsMessageRegExp = "Visual Regression Tracker has not been started")
    public void submitTestRunShouldThrowIfNotStarted() throws IOException, InterruptedException {
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
    public void trackShouldThrowException(TestRunResponse testRunResponse, String expectedExceptionMessage) throws IOException, InterruptedException {
        vrtMocked.configuration = VisualRegressionTrackerConfig.builder()
                .apiUrl("")
                .project("")
                .apiKey("")
                .branchName("")
                .enableSoftAssert(false)
                .ciBuildId("")
                .build();

        when(vrtMocked.submitTestRun(anyString(), anyString(), any())).thenReturn(testRunResponse);

        doCallRealMethod().when(vrtMocked).track(anyString(), anyString(), any());
        vrtMocked.track("name", "image", TestRunOptions.builder().build());
    }

    @Test(dataProvider = "trackErrorCases")
    public void trackShouldLogSevere(TestRunResponse testRunResponse, String expectedExceptionMessage) throws IOException, InterruptedException {
        Logger loggerMock = (Logger) LoggerFactory.getLogger(VisualRegressionTracker.class);
        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        // add the appender to the logger
        loggerMock.addAppender(listAppender);
        vrtMocked.configuration = VisualRegressionTrackerConfig.builder()
                .apiUrl("")
                .project("")
                .apiKey("")
                .branchName("")
                .enableSoftAssert(true)
                .ciBuildId("")
                .build();
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
    public void shouldTrackPass(TestRunResponse testRunResponse) throws IOException, InterruptedException {
        when(vrtMocked.submitTestRun(anyString(), anyString(), any())).thenReturn(testRunResponse);
        vrtMocked.paths = new PathProvider("http://localhost:4200");

        doCallRealMethod().when(vrtMocked).track(anyString(), anyString(), any());
        TestRunResult testRunResult = vrtMocked.track("name", "image", TestRunOptions.builder().build());

        assertThat(testRunResult.getTestRunResponse(), is(testRunResponse));
        assertThat(testRunResult.getImageUrl(), is("http://localhost:4200/".concat(testRunResponse.getImageName())));
        assertThat(testRunResult.getDiffUrl(), is("http://localhost:4200/".concat(testRunResponse.getDiffName())));
        assertThat(testRunResult.getBaselineUrl(), is("http://localhost:4200/".concat(testRunResponse.getBaselineName())));
    }

    @Test()
    public void shouldTrackOverload() throws IOException, InterruptedException {
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

        String exceptionMessage = "";
        try {
            //A mock client is needed to create a mock response
            HttpClient httpClient = new MockHttpClient(404, error);
            HttpResponse httpResponse = httpClient.send(null, null);
            vrt.handleResponse(httpResponse, Object.class);
        } catch (TestRunException | InterruptedException ex) {
            exceptionMessage = ex.getMessage();
        }

        assertThat(exceptionMessage, is(error));
    }

    @Test
    public void httpTimeoutWorks() throws IOException, InterruptedException {
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .projectId(PROJECT_ID)
                .ciBuildId(CI_BUILD_ID)
                .build();
        String json = gson.toJson(buildResponse);
        //body size is 97 bytes, http timeout is 1s, set body read delay to 0.5s, wait that vrt get all values correctly
        mockWebServer.enqueue(new MockResponse().throttleBody(json.length(), HTTP_TIMEOUT * 1000 - 500, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
                .setBody(json));

        vrt.start();

        mockWebServer.takeRequest();

        assertThat(vrt.buildId, is(BUILD_ID));
        assertThat(vrt.projectId, is(PROJECT_ID));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = "This method is not yet supported.")
    public void methodNotSupported() {
        vrt.getRequest(METHOD.GET, null, null);
    }

    @Test(expectedExceptions = HttpTimeoutException.class,
            expectedExceptionsMessageRegExp = "^(request timed out)$")
    public void httpTimeoutElapsed() throws IOException, InterruptedException {
        BuildResponse buildResponse = BuildResponse.builder()
                .id(BUILD_ID)
                .projectId(PROJECT_ID)
                .ciBuildId(CI_BUILD_ID)
                .build();
        String json = gson.toJson(buildResponse);
        //Send part of the body after every timeout, http timeout is 1s, set body read delay to 1s, wait for error
        //For some reason double HTTP_TIMEOUT does not throw exception.
        mockWebServer.enqueue(new MockResponse().throttleBody((json.length() / 2), HTTP_TIMEOUT, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody(json));

        vrt.start();

        mockWebServer.takeRequest();
    }
}
