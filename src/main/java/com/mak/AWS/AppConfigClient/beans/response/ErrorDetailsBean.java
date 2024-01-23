package com.mak.AWS.AppConfigClient.beans.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetailsBean {
    private String errorCode;
    private String errorDetails;
}
