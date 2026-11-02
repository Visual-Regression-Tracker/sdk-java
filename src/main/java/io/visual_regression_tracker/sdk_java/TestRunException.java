package io.visual_regression_tracker.sdk_java;

import lombok.Getter;

@Getter
public class TestRunException extends RuntimeException {
    static final String PAYLOAD_TOO_LARGE_HINT =
            "The request is above the server's JSON body limit (BODY_PARSER_JSON_LIMIT); send a smaller screenshot or raise the limit.";
    static final String IMAGE_TOO_LARGE_HINT =
            "The image has more pixels than the server accepts (MAX_IMAGE_PIXELS); send a smaller screenshot or raise the limit.";

    private final Integer statusCode;

    public TestRunException(String errorMessage) {
        super(errorMessage);
        this.statusCode = null;
    }

    public TestRunException(int statusCode, String serverMessage) {
        super(httpErrorMessage(statusCode, serverMessage));
        this.statusCode = statusCode;
    }

    private static String httpErrorMessage(int statusCode, String serverMessage) {
        String detail = serverMessage.isBlank() ? "" : ": " + serverMessage;
        String message = "Request failed with status " + statusCode + detail;
        switch (statusCode) {
            case 413:
                return message + ". " + PAYLOAD_TOO_LARGE_HINT;
            case 422:
                return message + ". " + IMAGE_TOO_LARGE_HINT;
            default:
                return message;
        }
    }
}
