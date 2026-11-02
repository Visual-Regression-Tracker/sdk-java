package io.visual_regression_tracker.sdk_java;

import org.testng.annotations.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class VisualRegressionTrackerConfigTest {

    private static final String REQUIRED_FIELDS = "{\"apiUrl\": \"http://localhost:4200\", \"apiKey\": \"KEY\", \"project\": \"PROJECT\"";

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
        assertThat(config.getHttpTimeoutInSeconds(), is(15));
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
        assertThat(config.getHttpTimeoutInSeconds(), is(360));
    }

    @Test
    public void shouldUseDefaultValuesWhenCreatedWithConstructor() {
        VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig("http://localhost:4200", "KEY", "PROJECT");

        assertThat(config.getEnableSoftAssert(), is(false));
        assertThat(config.getHttpTimeoutInSeconds(), is(360));
    }

    @Test
    public void shouldIgnoreNonScalarValuesInConfigFile() throws Exception {
        VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
                .configFile(configFile(REQUIRED_FIELDS + ", \"extra\": {\"nested\": 1}}"))
                .build();

        assertThat(config.getProject(), is("PROJECT"));
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "apiUrl is marked non-null but is null")
    public void shouldTreatEmptyConfigFileAsNoValues() throws Exception {
        VisualRegressionTrackerConfig.builder()
                .configFile(configFile(""))
                .build();
    }

    @Test
    public void shouldReadWholeNumberWrittenAsDecimalFromConfigFile() throws Exception {
        VisualRegressionTrackerConfig config = VisualRegressionTrackerConfig.builder()
                .configFile(configFile(REQUIRED_FIELDS + ", \"httpTimeoutInSeconds\": 15.0}"))
                .build();

        assertThat(config.getHttpTimeoutInSeconds(), is(15));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Invalid value for 'httpTimeoutInSeconds': 15.5")
    public void shouldNameTheKeyOfAnInvalidValue() throws Exception {
        VisualRegressionTrackerConfig.builder()
                .configFile(configFile(REQUIRED_FIELDS + ", \"httpTimeoutInSeconds\": 15.5}"))
                .build();
    }

    private File configFile(String content) throws Exception {
        Path configFile = Files.createTempFile("vrt", ".json");
        Files.writeString(configFile, content);
        return configFile.toFile();
    }
}
