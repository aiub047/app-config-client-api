package com.mak.AWS.AppConfigClient.beans.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mak.AWS.AppConfigClient.beans.AttributeBean;
import com.mak.AWS.AppConfigClient.beans.FlagBean;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagRequestBean {
    @NotNull(message = "The applicationId is required.")
    @NotEmpty(message = "The applicationId is required.")
    private String applicationId;
    @NotNull(message = "The environmentId is required.")
    @NotEmpty(message = "The environmentId is required.")
    private String environmentId;
    @NotNull(message = "The profileId is required.")
    @NotEmpty(message = "The profileId is required.")
    private String profileId;
    @NotNull(message = "The flag(s) is/are required.")
    @NotEmpty(message = "The flag(s) is/are required.")
    private List<FlagBean> flags;
    private int version;
}
