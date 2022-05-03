package com.redhat.developer.demos.recommendation.rest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;


@ApplicationScoped
@Path("/")
public class RecommendationEndpoint {

    private static final String RESPONSE_STRING_FORMAT = "precio v2 desde '%s': %d";

    private static final String HOSTNAME = parseContainerIdFromHostname(
            System.getenv().getOrDefault("HOSTNAME", "unknown")
    );

    static String parseContainerIdFromHostname(String hostname) {
        return hostname.replaceAll("precio-v\\d+-", "");
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Counter to help us see the lifecycle
     */
    private int count = 0;

    /**
     * Flag for enabling timeout
     */
    private boolean timeout = false;

    /**
     * Flag for throwing a 503 when enabled
     */
    private boolean misbehave = false;

    @GET
    @Produces("text/plain")
    public Response getRecommendation(@HeaderParam("user-agent") String userAgent) throws InterruptedException {
        logger.info(String.format("solicitud de precio desde %s: %d", HOSTNAME, count));
        if (misbehave) {
            count = 0;
            logger.info(String.format("Misbehaving %d", count));
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(String.format("precio misbehavior desde '%s'\n", HOSTNAME))
                    .build();
        } else {
            count++;
            if (timeout){
                Thread.sleep(3000);
            }
            return Response
                    .ok(String.format(RESPONSE_STRING_FORMAT, HOSTNAME, count))
                    .build();
        }
    }

    @GET
    @Produces("text/plain")
    @Path("/timeout")
    public Response timeout(){
        this.timeout = true;
        return Response.ok("Las siguientes solicitudes a '/' demorarán 3 segundos\n").build();
    }

    @GET
    @Produces("text/plain")
    @Path("/misbehave")
    public Response misbehave(){
        this.misbehave = true;
        logger.info("'misbehave' se ha establecido en 'true'");
        return Response.ok("Las siguientes solicitudes a '/' devolverán un 503\n").build();
    }

    @GET
    @Produces("text/plain")
    @Path("/behave")
    public Response behave(){
        this.misbehave = false;
        logger.info("'misbehave' se ha establecido en 'false'");
        return Response.ok("Las siguientes solicitudes a '/' devolverán un 200\n").build();
    }

}
