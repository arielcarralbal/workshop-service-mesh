package com.redhat.workshop.producto.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
public class ProductoController {

    private static final String RESPONSE_STRING_FORMAT = "producto => %s\n";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate;

    @Value("${precio.api.url:http://precio:8080}")
    private String remoteURL;

    public ProductoController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/")
    public ResponseEntity<?> getProductoData() {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(remoteURL, String.class);
            String response = responseEntity.getBody();
            return ResponseEntity.ok(String.format(RESPONSE_STRING_FORMAT, response != null ? response.trim() : ""));
        } catch (HttpStatusCodeException ex) {
            logger.warn("HttpStatusCodeException intentando obtener la respuesta del servicio PRECIOS.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT,
                            String.format("%d %s", ex.getRawStatusCode(), createHttpErrorResponseString(ex))));
        } catch (RestClientException ex) {
            logger.warn("RestClientException intentando obtener la respuesta del servicio PRECIOS.", ex);
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
