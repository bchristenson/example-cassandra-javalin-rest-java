package com.kineticdata.examples.javalin;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import com.kineticdata.examples.javalin.controllers.WidgetController;
import com.kineticdata.examples.javalin.daos.WidgetDao;
import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;
import io.javalin.json.JavalinJackson;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleApp {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApp.class);
    private static final ImmutableMap VERSION_PAYLOAD = ImmutableMap.of("version", "v1.0.0-SNAPSHOT");
    
    public static void main(String[] args) throws Exception {
        start(null);
    }
    
    public static void start(
        Runnable afterStartupCallback
    ) {
        // Prepare a shutdown hook
        CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownFuture.complete((Void)null);
        }));
        LOGGER.info("Starting...");
        
        // Prepare the Javalin object
        Javalin javalin = Javalin.create();
        // Start the application
        try (
            // Prepare the Cassandra cluster and session objects
            Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
            Session session = cluster.connect("cassandra_javalin_example");
        ) {
            // Prepare the daos
            WidgetDao widgetDao = new WidgetDao(session);
            // Prepare the controllers
            WidgetController widgets = new WidgetController(widgetDao);
            
            // Configure Javalin
            javalin
                .port(3000)
                .defaultContentType("application/json")
                .enableCaseSensitiveUrls()
                .before(context -> {
                    context.attribute("start", System.currentTimeMillis());
                })
                .after(context -> {
                    long duration = System.currentTimeMillis()-(Long)context.attribute("start");
                    String queryString = (context.queryString() == null)
                        ? "" 
                        : "?"+context.queryString();
                    LOGGER.info(
                        context.method()+" "+
                        context.path()+queryString+" "+ 
                        context.status()+" ("+duration+"ms)");
                })
                .routes(() -> {
                    path("/app/api/v1", () -> {
                        get("/version", context -> {
                            CompletableFuture<String> result = CompletableFuture
                                .completedFuture(VERSION_PAYLOAD)
                                .thenApply(payload -> JavalinJackson.INSTANCE.toJson(payload));
                            context.result(result);
                        });
                        
                        path("/tenants/:tenantKey", () -> {
                            get("/widgets", widgets::list);
                            post("/widgets", widgets::create);
                            get("/widgets/:key", widgets::retrieve);
                            put("/widgets/:key", widgets::update);
                            delete("/widgets/:key", widgets::delete);
                        });
                    });
                })
                .exception(Exception.class, (e, context) -> {
                    // Because the controller actions are async completable futures, they are
                    // wrapped in a CompletionException error
                    if (e instanceof CompletionException && e.getCause() instanceof Exception) {
                        e = (Exception)e.getCause();
                    }
                    // Log the exception
                    LOGGER.error("There was a problem handling the request.", e);
                    // Set the results
                    context.status(500);
                    context.json(ImmutableMap.of("error", e.getMessage()));
                })
                .start();
            
            // Execute the after startup callback
            if (afterStartupCallback != null) {
                afterStartupCallback.run();
            }
            
            // Await termination of the JVM
            shutdownFuture.join();
            // Shutdown the webserver
            javalin.stop();
        } catch (Exception e) {
            LOGGER.info("Unexpected exception encountered.", e);
        } finally {
            LOGGER.info("Stopped.");
        }
    }
    
}
