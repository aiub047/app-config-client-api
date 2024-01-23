package com.mak.AWS.AppConfigClient.beans;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;


/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FlagBean {
    @NotNull(message = "The flagKey is required")
    @NotEmpty(message = "The flagKey is required")
    private String flagKey;
    @NotNull(message = "The enabled is required")
    @NotEmpty(message = "The enabled is required")
    private String enabled;
    private List<AttributeBean> attributes;
}
