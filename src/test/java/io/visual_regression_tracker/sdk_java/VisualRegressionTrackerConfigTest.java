package io.visual_regression_tracker.sdk_java;

import org.testng.annotations.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class VisualRegressionTrackerConfigTest {

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "apiKey is marked non-null but is null")
    public void shouldThrowExceptionIfApiKeyMissed() {
        VisualRegressionTrackerConfig
                .builder()
                .apiUrl("")
                .project("")
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "apiUrl is marked non-null but is null")
    public void shouldThrowExceptionIfApiUrlMissed() {
        VisualRegressionTrackerConfig
                .builder()
                .apiKey("")
                .project("")
                .build();
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "project is marked non-null but is null")
    public void shouldThrowExceptionIfProjectMissed() {
        VisualRegressionTrackerConfig
                .builder()
                .apiKey("")
                .apiUrl("")
                .build();
    }

    @Test
    public void shouldBeCreatedFromConfigFile() {
        File configFile = new File("src/test/resources/vrt_config_example.json");

        VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
                .configFile(configFile)
                .build();

        assertThat(config.getApiKey(), is("SECRET"));
        assertThat(config.getApiUrl(), is("http://162.243.161.172:4200"));
        assertThat(config.getProject(), is("VRT"));
        assertThat(config.getBranchName(), is("master"));
        assertThat(config.getEnableSoftAssert(), is(false));
        assertThat(config.getCiBuildId(), is("SOME_UNIQUE_ID"));
    }

    @Test
    public void shouldBeCreatedFromEnvironment() throws Exception {
        EnvironmentVariables environmentVariables = new EnvironmentVariables("VRT_APIKEY", "SECRET")
                .set("VRT_APIURL", "http://162.243.161.172:4200")
                .set("VRT_PROJECT", "VRT")
                .set("VRT_BRANCHNAME", "master")
                .set("VRT_ENABLESOFTASSERT", "false")
                .set("VRT_CIBUILDID", "SOME_UNIQUE_ID");

        VisualRegressionTrackerConfig config = environmentVariables.execute(() ->
                VisualRegressionTrackerConfig.builder()
                        .build()
        );

        assertThat(config.getApiKey(), is("SECRET"));
        assertThat(config.getApiUrl(), is("http://162.243.161.172:4200"));
        assertThat(config.getProject(), is("VRT"));
        assertThat(config.getBranchName(), is("master"));
        assertThat(config.getEnableSoftAssert(), is(false));
        assertThat(config.getCiBuildId(), is("SOME_UNIQUE_ID"));
    }

    @Test
    public void shouldResolveFinalValuesInTheRightOrder() throws Exception {
        EnvironmentVariables environmentVariables = new EnvironmentVariables("VRT_APIKEY", "ENV_SECRET")
                .set("VRT_APIURL", "http://162.243.161.172:4201");
        File configFile = new File("src/test/resources/vrt_config_example.json");

        VisualRegressionTrackerConfig config = environmentVariables.execute(() ->
                VisualRegressionTrackerConfig.builder()
                        .apiUrl("http://localhost:4200")
                        .configFile(configFile)
                        .build()
        );

        assertThat(config.getApiKey(), is("ENV_SECRET"));
        assertThat(config.getApiUrl(), is("http://localhost:4200"));
        assertThat(config.getProject(), is("VRT"));
        assertThat(config.getBranchName(), is("master"));
        assertThat(config.getEnableSoftAssert(), is(false));
        assertThat(config.getCiBuildId(), is("SOME_UNIQUE_ID"));
    }


    @Test
    public void shouldUseDefaultValuesIfNotProvided() {
        VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
                        .apiUrl("http://localhost:4200")
                        .apiKey("KEY")
                        .project("PROJECT")
                        .build();

        assertThat(config.getEnableSoftAssert(), is(false));
        assertThat(config.getHttpTimeoutInSeconds(), is(10));
    }
}
