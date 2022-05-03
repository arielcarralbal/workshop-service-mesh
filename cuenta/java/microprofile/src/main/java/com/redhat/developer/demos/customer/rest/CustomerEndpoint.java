package com.redhat.developer.demos.customer.rest;

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
public class CustomerEndpoint {

    private static final String RESPONSE_STRING_FORMAT = "cuenta => %s\n";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @ConfigProperty(name = "productos.api.url", defaultValue = "http://producto:8080")
    private String remoteURL;

    @GET
    @Produces("text/plain")
    public Response getCustomer(@HeaderParam("user-agent") String userAgent) {
        try {
            Client client = ClientBuilder.newClient();
            Response res = client.target(remoteURL).request("text/plain").header("user-agent", userAgent).get();
            if (res.getStatus() == Response.Status.OK.getStatusCode()){
                return Response.ok(String.format(RESPONSE_STRING_FORMAT, res.readEntity(String.class))).build();
            }else{
                logger.warn("No HTTP 20x tratando de obtener la respuesta del servicio de producto: " + res.getStatus());
                return Response
                        .status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(String.format(RESPONSE_STRING_FORMAT,
                                String.format("Error: %d - %s", res.getStatus(), res.readEntity(String.class)))
                        )
                        .build();
            }
        } catch (ProcessingException ex) {
            logger.warn("Excepción al intentar obtener la respuesta del servicio de producto.", ex);
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(String.format(RESPONSE_STRING_FORMAT, ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage()))
                    .build();
        }
    }
}
