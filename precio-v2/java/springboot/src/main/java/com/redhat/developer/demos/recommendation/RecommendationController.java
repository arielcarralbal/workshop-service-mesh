package com.redhat.developer.demos.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {

    private static final String RESPONSE_STRING_FORMAT = "precio v2 from '%s': %d\n";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Counter to help us see the lifecycle
     */
    private int count = 0;

    /**
     * Flag for throwing a 503 when enabled
     */
    private boolean misbehave = false;

    private boolean slow = false;

    private static final String HOSTNAME = parseContainerIdFromHostname(
            System.getenv().getOrDefault("HOSTNAME", "host desconocido"));

    static String parseContainerIdFromHostname(String hostname) {
        return hostname.replaceAll("precio-v\\d+-", "");
    }

    @RequestMapping("/")
    public ResponseEntity<String> getRecommendations() {
        count++;
        logger.info(String.format("precio desde %s: %d", HOSTNAME, count));

        if (slow) {
            timeout();
        }

        if (misbehave) {
            return doMisbehavior();
        }
        return ResponseEntity.ok(String.format(RecommendationController.RESPONSE_STRING_FORMAT, HOSTNAME, count));
    }

    private void timeout() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.info("Thread interrupted");
        }
    }

    private ResponseEntity<String> doMisbehavior() {
        logger.debug(String.format("Misbehaving %d", count));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(String.format("Precio misbehavior desde '%s'\n", HOSTNAME));
    }

    @RequestMapping("/misbehave")
    public ResponseEntity<String> flagMisbehave() {
        this.misbehave = true;
        logger.debug("'misbehave' has been set to 'true'");
        return ResponseEntity.ok("Next request to / will return a 503\n");
    }

    @RequestMapping("/behave")
    public ResponseEntity<String> flagBehave() {
        this.misbehave = false;
        logger.debug("'misbehave' has been set to 'false'");
        return ResponseEntity.ok("Next request to / will return 200\n");
    }

    @RequestMapping("/slow")
    public ResponseEntity<String> flagSlow() {
        this.slow = true;
        logger.debug("'slow' has been set to 'true'");
        return ResponseEntity.ok("Next request to / will be slow\n");
    }

    @RequestMapping("/fast")
    public ResponseEntity<String> flagFast() {
        this.slow = false;
        logger.debug("'slow' has been set to 'false'");
        return ResponseEntity.ok("Next request to / will be fast\n");
    }

}
