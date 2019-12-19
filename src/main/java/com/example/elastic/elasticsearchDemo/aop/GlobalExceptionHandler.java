package com.example.elastic.elasticsearchDemo.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> errorHandle(HttpServletRequest request, Exception e) {
        log.info("request method: {}", request.getMethod());
        log.info("request url: {}", request.getRequestURL());
        log.error("Ops!", e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.OK);
    }

}
