package io.visual_regression_tracker.sdk_java;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@Data()
@RequiredArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Slf4j
public class VisualRegressionTrackerConfig {

    @NonNull
    private final String apiUrl;
    @NonNull
    private final String apiKey;
    @NonNull
    private final String project;

    private String branchName;
    private String ciBuildId;
    private Boolean enableSoftAssert;
    private int httpTimeoutInSeconds;

    public static VisualRegressionTrackerConfigBuilder builder() {
        return new VisualRegressionTrackerConfigBuilder();
    }

    public static class VisualRegressionTrackerConfigBuilder {
        private String apiUrl;
        private String apiKey;
        private String project;

        private String branchName;
        private String ciBuildId;
        private Boolean enableSoftAssert;
        private Integer httpTimeoutInSeconds;

        private File configFile;

        private static final String VRT_ENV_VARIABLE_PREFIX = "VRT_";
        private static final boolean DEFAULT_SOFT_ASSERTION_STATE = false;
        private static final int DEFAULT_HTTP_TIMEOUT_SECONDS = 10;

        public VisualRegressionTrackerConfigBuilder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder project(String project) {
            this.project = project;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder ciBuildId(String ciBuildId) {
            this.ciBuildId = ciBuildId;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder enableSoftAssert(Boolean enableSoftAssert) {
            this.enableSoftAssert = enableSoftAssert;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder httpTimeoutInSeconds(int httpTimeoutInSeconds) {
            this.httpTimeoutInSeconds = httpTimeoutInSeconds;
            return this;
        }

        public VisualRegressionTrackerConfigBuilder configFile(File configFile) {
            this.configFile = configFile;
            return this;
        }

        public VisualRegressionTrackerConfig build() {
            Map<String, Object> configFromFile = Collections.emptyMap();
            if (configFile != null) {
                configFromFile = readConfigFromFile(configFile);
            }

            String actualApiUrl = resolve("apiUrl", configFromFile);
            String actualApiKey = resolve("apiKey", configFromFile);
            String actualProject = resolve("project", configFromFile);

            VisualRegressionTrackerConfig config = new VisualRegressionTrackerConfig(actualApiUrl, actualApiKey, actualProject);
            config.setCiBuildId(resolve("ciBuildId", configFromFile));
            config.setBranchName(resolve("branchName", configFromFile));

            Boolean actualEnableSoftAssert = resolve("enableSoftAssert", configFromFile);
            config.setEnableSoftAssert(actualEnableSoftAssert == null ? DEFAULT_SOFT_ASSERTION_STATE : actualEnableSoftAssert);

            Integer actualHttpTimeoutInSeconds = resolve("httpTimeoutInSeconds", configFromFile);
            config.setHttpTimeoutInSeconds(actualHttpTimeoutInSeconds == null ? DEFAULT_HTTP_TIMEOUT_SECONDS : actualHttpTimeoutInSeconds);

            return config;
        }

        private Map<String, Object> readConfigFromFile(File configFile) {
            if (!configFile.exists()) {
                throw new IllegalArgumentException("File " + configFile + " doesn't exist");
            }

            String fileContent;
            try {
                fileContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't read content of provided config file", e);
            }
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(fileContent, mapType);
        }

        @SneakyThrows
        private <T> T resolve(String propertyName, Map<String, Object> configurationFromFile) {
            // 1. check if it was initialized explicitly in builder
            // 2. check if env variable exists
            // 3. try to read from file as last resort
            Field field = this.getClass().getDeclaredField(propertyName);
            Object propertyValue = field.get(this);
            if (propertyValue != null) {
                return (T) propertyValue;
            }

            String environmentVariableName = VRT_ENV_VARIABLE_PREFIX +  propertyName.toUpperCase();
            propertyValue = System.getenv(environmentVariableName);
            if (propertyValue != null) {
                log.debug("Value of '{}' resolved from environment variable {}", propertyName, environmentVariableName);
                Function<String, ?> parser = findParser(field.getType());
                return (T) parser.apply((String)propertyValue);
            }

            propertyValue = configurationFromFile.get(propertyName);
            if (propertyValue != null) {
                log.debug("Value of '{}' resolved from config file", propertyName);
            }
            return propertyValue == null ? null : (T) propertyValue;
        }

        private Function<String, ?> findParser(Class<?> cls) {
            if (cls.equals(Boolean.class)) {
                return Boolean::parseBoolean;
            }
            if (cls.equals(Integer.class)) {
                return Integer::parseInt;
            }
            return String::valueOf;
        }
    }

}
