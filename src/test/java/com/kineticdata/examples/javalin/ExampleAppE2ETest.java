package com.kineticdata.examples.javalin;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;
import java.util.List;
import java.util.function.Consumer;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import testing.kineticdata.examples.javalin.E2ETestBase;

public class ExampleAppE2ETest extends E2ETestBase {
    
    public String tenant;
    
    @Before
    public void beforeEach() {
        tenant = "acme-"+System.currentTimeMillis();
    }
    
    /*----------------------------------------------------------------------------------------------
     * TESTS
     *--------------------------------------------------------------------------------------------*/
    
    @Test
    public void test_SanityCheck() throws Exception {
        // List
        request(
            Unirest.get(url("/app/api/v1/tenants/"+tenant+"/widgets")), 
            200, 
            response -> {
                assertEquals(0, ((List)response.read("$.widgets")).size());
            });
        
        // CREATE
        request(
            Unirest.post(url("/app/api/v1/tenants/"+tenant+"/widgets"))
                .body(new JSONObject()
                    .put("key", "foo")
                    .put("description", "A fooish widget.")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widget.tenantKey"));
                assertEquals("foo", response.read("$.widget.key"));
                assertEquals("A fooish widget.", response.read("$.widget.description"));
            });
        
        // LIST
        request(
            Unirest.get(url("/app/api/v1/tenants/"+tenant+"/widgets")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widgets[0].tenantKey"));
                assertEquals("foo", response.read("$.widgets[0].key"));
                assertEquals("A fooish widget.", response.read("$.widgets[0].description"));
            });
        
        // RETRIEVE
        request(
            Unirest.get(url("/app/api/v1/tenants/"+tenant+"/widgets/foo")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widget.tenantKey"));
                assertEquals("foo", response.read("$.widget.key"));
                assertEquals("A fooish widget.", response.read("$.widget.description"));
            });
        
        // UPDATE
        request(
            Unirest.put(url("/app/api/v1/tenants/"+tenant+"/widgets/foo"))
                .body(new JSONObject()
                    .put("description", "A barish widget.")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widget.tenantKey"));
                assertEquals("foo", response.read("$.widget.key"));
                assertEquals("A barish widget.", response.read("$.widget.description"));
            });
        
        // UPDATE (REINSERT)
        request(
            Unirest.put(url("/app/api/v1/tenants/"+tenant+"/widgets/foo"))
                .body(new JSONObject()
                    .put("key", "foo-2")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widget.tenantKey"));
                assertEquals("foo-2", response.read("$.widget.key"));
                assertEquals("A barish widget.", response.read("$.widget.description"));
            });
        
        // LIST
        request(
            Unirest.get(url("/app/api/v1/tenants/"+tenant+"/widgets")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widgets[0].tenantKey"));
                assertEquals("foo-2", response.read("$.widgets[0].key"));
                assertEquals("A barish widget.", response.read("$.widgets[0].description"));
            });
        
        // DELETE
        request(
            Unirest.delete(url("/app/api/v1/tenants/"+tenant+"/widgets/foo-2")), 
            200, 
            response -> {
                assertEquals(tenant, response.read("$.widget.tenantKey"));
                assertEquals("foo-2", response.read("$.widget.key"));
                assertEquals("A barish widget.", response.read("$.widget.description"));
            });
        
        // List
        request(
            Unirest.get(url("/app/api/v1/tenants/"+tenant+"/widgets")), 
            200, 
            response -> {
                assertEquals(0, ((List)response.read("$.widgets")).size());
            });
    }
    
    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
    
    protected void request(
        BaseRequest request, 
        int expectedStatus,
        Consumer<DocumentContext> consumer
    ) throws UnirestException {
        HttpResponse<String> response = request.asString();
        assertEquals(
            "Unexpected response status for:\n  "
                +request.getHttpRequest().getHttpMethod()+" "+request.getHttpRequest().getUrl()+"\n  "
                +response.getBody(),
            expectedStatus,
            response.getStatus());
        consumer.accept(JsonPath.parse(response.getBody()));
    }

}
