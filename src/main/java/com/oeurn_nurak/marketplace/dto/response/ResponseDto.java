package com.oeurn_nurak.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDto<T> {
    private final int status;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ResponseDto(int status, String message, T data) {
        this.status    = status;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ResponseDto<T> ok(T data) {
        return new ResponseDto<>(200, "Success", data);
    }

    public static <T> ResponseDto<T> ok(String message, T data) {
        return new ResponseDto<>(200, message, data);
    }

    public static <T> ResponseDto<T> created(String message, T data) {
        return new ResponseDto<>(201, message, data);
    }

    public static <T> ResponseDto<T> message(String message) {
        return new ResponseDto<>(200, message, null);
    }

    public static <T> ResponseDto<T> error(HttpStatus status, String message, T data) {
        return new ResponseDto<>(status.value(), message, data);
    }
}
