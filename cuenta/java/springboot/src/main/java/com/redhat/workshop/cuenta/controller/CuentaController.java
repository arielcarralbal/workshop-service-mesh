package com.redhat.workshop.cuenta.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
public class CuentaController {

    private static final String RESPONSE_STRING_FORMAT = "cuenta => %s\n";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate;

    @Value("${producto.api.url:http://producto:8080}")
    private String remoteURL;

    public CuentaController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/")
    public ResponseEntity<String> getCuenta(@RequestHeader("User-Agent") String userAgent) {
        try {
            // tracer.activeSpan().setBaggageItem("user-agent", userAgent);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(remoteURL, String.class);
            String response = responseEntity.getBody();
            return ResponseEntity.ok(String.format(RESPONSE_STRING_FORMAT, response != null ? response.trim() : ""));
        } catch (HttpStatusCodeException ex) {
            logger.warn("HttpStatusCodeException intentando obtener la respuesta del servicio PRODUCTOS.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT,
                            String.format("%d %s", ex.getRawStatusCode(), createHttpErrorResponseString(ex))));
        } catch (RestClientException ex) {
            logger.warn("RestClientException intentando obtener la respuesta del servicio PRODUCTOS.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT, ex.getMessage()));
        }
    }

    private String createHttpErrorResponseString(HttpStatusCodeException ex) {
        String responseBody = ex.getResponseBodyAsString().trim();
        if (responseBody.startsWith("null")) {
            return ex.getStatusCode().getReasonPhrase();
        }
        return responseBody;
    }
}
