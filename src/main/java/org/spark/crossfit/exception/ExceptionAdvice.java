package org.spark.crossfit.exception;

import org.spark.crossfit.dto.CommonResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<CommonResult<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        var result = CommonResult.<Void>failure(ex.getMessage());

        return ResponseEntity.status(401).body(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResult<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        var result = CommonResult.<Void>failure(ex.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResult<Void>> handleGeneralException(Exception ex) {
        var result = CommonResult.<Void>failure("Internal server error");
        return ResponseEntity.status(500).body(result);
    }
}
