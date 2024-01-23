package com.mak.AWS.AppConfigClient.beans;

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
public class ApplicationBean {
    private String id;
    private String name;
    private String description;
}
