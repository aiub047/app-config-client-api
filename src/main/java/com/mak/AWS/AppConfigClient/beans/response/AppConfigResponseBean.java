package com.mak.AWS.AppConfigClient.beans.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mak.AWS.AppConfigClient.beans.ApplicationBean;
import lombok.*;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */


@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppConfigResponseBean<T> {
    private String status;
    private ErrorDetailsBean errorDetails;
    private T data;
    private List<T> list;

    public AppConfigResponseBean(List<T> list, String status) {
        this.status = status;
        this.list = list;
    }

    public AppConfigResponseBean(T data, String status) {
        this.status = status;
        this.data = data;
    }
}
