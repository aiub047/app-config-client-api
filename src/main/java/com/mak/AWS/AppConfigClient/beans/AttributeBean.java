package com.mak.AWS.AppConfigClient.beans;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;


/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AttributeBean {
    @NotNull(message = "The key is required.")
    @NotEmpty(message = "The key is required.")
    private String key;
    @NotNull(message = "The type is required.")
    @NotEmpty(message = "The type is required.")
    private String type;
    @NotNull(message = "The value is required.")
    @NotEmpty(message = "The value is required.")
    private String value;
}
