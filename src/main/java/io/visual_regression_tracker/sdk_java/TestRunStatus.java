package io.visual_regression_tracker.sdk_java;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestRunStatus {
    @SerializedName("ok")
    OK,
    @SerializedName("approved")
    APPROVED,
    @SerializedName("autoApproved")
    AUTO_APPROVED,
    @SerializedName("failed")
    FAILED,
    @SerializedName("new")
    NEW,
    @SerializedName("unresolved")
    UNRESOLVED
}
