package com.mak.AWS.AppConfigClient.beans;

import lombok.*;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EnvironmentBean {
    private String applicationId;
    private String id;
    private String name;
    private String description;
    private String state;
}
