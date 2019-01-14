package com.kineticdata.examples.javalin.controllers;

import com.google.common.collect.ImmutableMap;
import com.kineticdata.examples.javalin.daos.WidgetDao;
import com.kineticdata.examples.javalin.models.Widget;
import io.javalin.Context;
import io.javalin.json.JavalinJackson;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WidgetController {

    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    
    private final WidgetDao widgetDao;
    
    public WidgetController(WidgetDao widgetDao) {
        this.widgetDao = widgetDao;
    }
    
    /*----------------------------------------------------------------------------------------------
     * METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public void list(Context context) {
        // Extract the request parameters
        String tenantKey = context.pathParam("tenantKey");
        Integer limit = context.validatedQueryParam("limit", "10").asInt().getOrThrow();
        String offsetKey = context.queryParam("offsetKey");
        // Asynchronously retrieve the widgets
        CompletableFuture<String> future = widgetDao.list(tenantKey, limit, offsetKey)
            // Once the widget query is complete, return the JSON string
            .thenApply(widgets -> toJson(ImmutableMap.of("widgets", widgets)));
        // Set the result future
        context.result(future);
    }
    
    public void create(Context context) {
        // Extract the request parameters
        String tenantKey = context.pathParam("tenantKey");
        // Prepare the widget from the body
        Widget.Builder builder = context.validatedBodyAsClass(Widget.Builder.class).getOrThrow();
        builder.setTenantKey(tenantKey);
        Widget model = builder.build();
        // Asynchronously create the widget
        CompletableFuture<String> future = widgetDao.create(model)
            // Once the widget query is complete, return the JSON string
            .thenApply(widget -> toJson(ImmutableMap.of("widget", widget)));
        // Set the result future
        context.result(future);
    }
    
    public void retrieve(Context context) {
        // Extract the request parameters
        String tenantKey = context.pathParam("tenantKey");
        String key = context.pathParam("key");
        // Asynchronously retrieve the widget
        CompletableFuture<String> future = widgetDao.retrieve(tenantKey, key)
            // Once the widget query is complete, raise an exception if it wasn't found
            .thenApply(optional -> optional.orElseThrow(() -> 
                new RuntimeException("The \""+key+"\" widget was not found.")))
            // Once the widget query is complete (and was found), return the JSON string
            .thenApply(widget -> toJson(ImmutableMap.of("widget", widget)));
        // Set the result future
        context.result(future);
    }
    
    public void update(Context context) {
        // Extract the request parameters
        String tenantKey = context.pathParam("tenantKey");
        String key = context.pathParam("key");
        // Prepare the widget from the body
        // Asynchronously retrieve the widget
        CompletableFuture<String> future = widgetDao.retrieve(tenantKey, key)
            // Once the widget query is complete, raise an exception if it wasn't found
            .thenApply(optional -> optional.orElseThrow(() -> 
                new RuntimeException("The \""+context.pathParam("id")+"\" widget was not found.")))
            // Once the widget query is complete (and was found), update the widget
            .thenCompose(persistedWidget -> {
                // Prepare the builder
                Widget.Builder builder = persistedWidget.builder();
                // Prepare the widget from the body
                Map<String,Object> body = fromJson(context.body(), Map.class);
                if (body.containsKey("tenantKey")) {
                    builder.setTenantKey((String)body.get("tenantKey"));
                }
                if (body.containsKey("key")) {
                    builder.setKey((String)body.get("key"));
                }
                if (body.containsKey("description")) {
                    builder.setDescription((String)body.get("description"));
                }
                return widgetDao.update(persistedWidget, builder.build());
            })
            // Once the widget query is complete, return the JSON string
            .thenApply(widget -> toJson(ImmutableMap.of("widget", widget)));
        // Set the result future
        context.result(future);
    }
    
    public void delete(Context context) {
        // Extract the request parameters
        String tenantKey = context.pathParam("tenantKey");
        String key = context.pathParam("key");
        // Asynchronously retrieve the widget
        CompletableFuture<String> future = widgetDao.retrieve(tenantKey, key)
            // Once the widget query is complete, raise an exception if it wasn't found
            .thenApply(optional -> optional.orElseThrow(() -> 
                new RuntimeException("The \""+key+"\" widget was not found.")))
            // Once the widget query is complete (and was found), delete the widget
            .thenCompose(widget -> widgetDao.delete(widget))
            // Once the delete query is complete, return the JSON string
            .thenApply(widget -> toJson(ImmutableMap.of("widget", widget)));
        // Set the result future
        context.result(future);
    }
    
    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public <T> T fromJson(String json, Class<T> objectClass) {
        return JavalinJackson.INSTANCE.fromJson(json, objectClass);
    }
    
    public String toJson(Object object) {
        return JavalinJackson.INSTANCE.toJson(object);
    }
    
}
