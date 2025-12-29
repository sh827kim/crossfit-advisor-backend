package org.spark.crossfit.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;

@Getter
@Setter
@ToString
public class CommonResult<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setSuccess(true);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    public static <T> CommonResult<T> failure(String... message) {
        if (message == null || message.length == 0) {
            message = new String[]{"Failure"};
        }

        CommonResult<T> result = new CommonResult<>();
        result.setSuccess(false);
        result.setMessage(Arrays.toString(message));
        result.setData(null);
        return result;
    }
}
