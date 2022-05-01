package com.redhat.workshop.precio;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class PrecioVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_STRING_FORMAT = "precio v2 desde '%s': %d\n";
    private static final String HTTP_NOW = "now.httpbin.org";

    private static final String HOSTNAME = parseContainerIdFromHostname(
            System.getenv().getOrDefault("HOSTNAME", "desconocido")
    );

    private static final int LISTEN_ON = Integer.parseInt(
            System.getenv().getOrDefault("LISTEN_ON", "8080")
    );

    static String parseContainerIdFromHostname(String hostname) {
        return hostname.replaceAll("precio-v\\d+-", "");
    }

    // Contador de peticiones
    private int count = 0;

    // Flag para arrojar 503
    private boolean misbehave = false;

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.get("/").handler(this::timeout); // pre-comentado
        router.get("/").handler(this::logging);
        router.get("/").handler(this::getPrecioData);
        router.get("/").handler(this::getNow); // pre-comentado
        router.get("/misbehave").handler(this::misbehave);
        router.get("/behave").handler(this::behave);

        HealthCheckHandler hc = HealthCheckHandler.create(vertx);
        hc.register("dummy-health-check", future -> future.complete(Status.OK()));
        router.get("/health").handler(hc);

        vertx.createHttpServer().requestHandler(router::accept).listen(LISTEN_ON);
    }

    private void logging(RoutingContext ctx) {
        logger.info(String.format("Request de Precio desde %s: %d", HOSTNAME, count));
        ctx.next();
    }

    private void timeout(RoutingContext ctx) {
        ctx.vertx().setTimer(3000, handler -> ctx.next());
    }

    private void getPrecioData(RoutingContext ctx) {
        if (misbehave) {
            count = 0;
            logger.info(String.format("Misbehaving %d", count));
            ctx.response().setStatusCode(503).end(String.format("Precio misbehavior desde '%s'\n", HOSTNAME));
        } else {
            count++;
            ctx.response().end(String.format(RESPONSE_STRING_FORMAT, HOSTNAME, count));
        }
    }

    private void getNow(RoutingContext ctx) {
        count++;
        final WebClient client = WebClient.create(vertx);
        client.get(80, HTTP_NOW, "/")
                .timeout(5000)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<JsonObject> response = ar.result();
                        JsonObject body = response.body();
                        String now = body.getJsonObject("now").getString("rfc2822");
                        ctx.response().end(now + " " + String.format(RESPONSE_STRING_FORMAT, HOSTNAME, count));
                    } else {
                        ctx.response().setStatusCode(503).end(ar.cause().getMessage());
                    }
                });
    }

    private void misbehave(RoutingContext ctx) {
        this.misbehave = true;
        logger.info("'misbehave' configurado en 'true'");
        ctx.response().end("Siguientes requests a '/' devolverán 503\n");
    }

    private void behave(RoutingContext ctx) {
        this.misbehave = false;
        logger.info("'misbehave' configurado en 'false'");
        ctx.response().end("Siguientes requests a '/' devolverá 200\n");
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new PrecioVerticle());
    }

}
