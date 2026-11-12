package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import io.visual_regression_tracker.sdk_java.response.BuildResponse;
import lombok.SneakyThrows;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.expectThrows;

public class VisualRegressionTrackerRetryTest {

    private final Gson gson = new Gson();
    private final List<Duration> waits = new ArrayList<>();
    private MockWebServer mockWebServer;
    private VisualRegressionTracker vrt;

    @SneakyThrows
    @BeforeMethod
    public void setup() {
        waits.clear();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        vrt = new VisualRegressionTracker(new VisualRegressionTrackerConfig(
                mockWebServer.url("/").toString(), "key", "project", "develop", null, false, 1)) {
            @Override
            void sleep(Duration duration) {
                waits.add(duration);
            }
        };
    }

    @SneakyThrows
    @AfterMethod
    public void tearDown() {
        mockWebServer.close();
    }

    @Test
    public void shouldRetryServiceUnavailableAfterTheTimeTheServerAsks() throws Exception {
        mockWebServer.enqueue(busy("2"));
        mockWebServer.enqueue(started());

        BuildResponse build = vrt.start();

        assertThat(build.getId(), is("buildId"));
        assertThat(mockWebServer.getRequestCount(), is(2));
        assertThat(waits, is(List.of(Duration.ofSeconds(2))));
    }

    @Test
    public void shouldCapTheWaitBetweenRetries() throws Exception {
        mockWebServer.enqueue(busy("3600"));
        mockWebServer.enqueue(started());

        vrt.start();

        assertThat(waits, is(List.of(VisualRegressionTracker.MAX_RETRY_AFTER)));
    }

    @Test
    public void shouldGiveUpAfterTheLastRetry() {
        for (int i = 0; i <= VisualRegressionTracker.MAX_RETRIES; i++) {
            mockWebServer.enqueue(busy("0"));
        }

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.start());

        assertThat(exception.getStatusCode(), is(503));
        assertThat(mockWebServer.getRequestCount(), is(VisualRegressionTracker.MAX_RETRIES + 1));
    }

    @Test
    public void shouldNotRetryServiceUnavailableWithoutRetryAfter() {
        mockWebServer.enqueue(new MockResponse.Builder().code(503).body("{\"message\":\"Directory service is not available.\"}").build());
        mockWebServer.enqueue(started());

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.start());

        assertThat(exception.getStatusCode(), is(503));
        assertThat(mockWebServer.getRequestCount(), is(1));
    }

    @Test
    public void shouldNotRetryOtherErrorsWithRetryAfter() {
        mockWebServer.enqueue(new MockResponse.Builder().code(429).setHeader("Retry-After", "0").build());
        mockWebServer.enqueue(started());

        expectThrows(TestRunException.class, () -> vrt.start());

        assertThat(mockWebServer.getRequestCount(), is(1));
    }

    @Test
    public void shouldNotRetryWhenRetryAfterIsNotSeconds() {
        mockWebServer.enqueue(busy("Wed, 21 Oct 2026 07:28:00 GMT"));
        mockWebServer.enqueue(started());

        TestRunException exception = expectThrows(TestRunException.class, () -> vrt.start());

        assertThat(exception.getStatusCode(), is(503));
        assertThat(mockWebServer.getRequestCount(), is(1));
    }

    private static MockResponse busy(String retryAfter) {
        return new MockResponse.Builder()
                .code(503)
                .setHeader("Retry-After", retryAfter)
                .body("{\"message\":\"Image diff queue is full (100 jobs)\"}")
                .build();
    }

    private MockResponse started() {
        return new MockResponse.Builder().body(gson.toJson(BuildResponse.builder().id("buildId").projectId("projectId").build())).build();
    }
}
