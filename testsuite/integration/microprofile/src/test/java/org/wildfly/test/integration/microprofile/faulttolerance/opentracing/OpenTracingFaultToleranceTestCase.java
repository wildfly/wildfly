package org.wildfly.test.integration.microprofile.faulttolerance.opentracing;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockTracer;
import java.io.FilePermission;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.microprofile.faulttolerance.opentracing.application.HelloResource;
import org.wildfly.test.integration.microprofile.faulttolerance.opentracing.application.MockTracerFactory;

@RunWith(Arquillian.class)
@RunAsClient
public class OpenTracingFaultToleranceTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(name = "opentracing-fault-tolerance")
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackage(MockTracer.class.getPackage())
                .addPackage(MockTracerFactory.class.getPackage())
                .addAsServiceProvider(TracerFactory.class, MockTracerFactory.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new FilePermission("<<ALL FILES>>", "read")
                ), "permissions.xml");
        return war;
    }

    @Test
    public void testHello() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url.toExternalForm() + "rest/hello"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Hello from fallback", content);
            response = client.execute(new HttpGet(url.toExternalForm() + "rest/tracer/spans"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            StringTokenizer tokenizer = new StringTokenizer(EntityUtils.toString(response.getEntity()), ";");
            Assert.assertEquals(4, tokenizer.countTokens());
            List<JsonObject> spans = new ArrayList<>(4);
            while (tokenizer.hasMoreTokens()) {
                String jsonContent = tokenizer.nextToken();
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonContent))) {
                    spans.add(jsonReader.readObject());
                }
            }
            int parentId = 0;
            Collections.sort(spans, (o1, o2) -> {
                return Integer.parseInt(o1.getString("spanId")) - Integer.parseInt(o2.getString("spanId"));
            });
            for (int i = 0; i < spans.size(); i++) {
                JsonObject object = spans.get(i);
                String operationName = object.getString("operationName");
                Assert.assertNotNull(object.toString(), object.getString("traceId"));
                Assert.assertNotNull(object.toString(), object.getString("spanId"));
                Assert.assertEquals(object.toString(), parentId, Integer.parseInt(object.getString("parentId")));
                switch (i) {
                    case 0: {
                        Assert.assertEquals(object.toString(), operationName, "GET:" + HelloResource.class.getCanonicalName() + ".get");
                        parentId = Integer.parseInt(object.getString("spanId"));
                        break;
                    }
                    case 1: {
                        Assert.assertEquals(object.toString(), operationName, "hello");
                        JsonObject log = object.getJsonObject("logs");
                        Assert.assertNotNull(object.toString(), log);
                        String event = log.getString("event");
                        Assert.assertTrue(event, "attempt 0".equals(event));
                        break;
                    }
                    case 2: {
                        Assert.assertEquals(object.toString(), operationName, "hello");
                        JsonObject log = object.getJsonObject("logs");
                        Assert.assertNotNull(object.toString(), log);
                        String event = log.getString("event");
                        Assert.assertTrue(event, "attempt 1".equals(event));
                        break;
                    }
                    case 3:
                        Assert.assertEquals(object.toString(), operationName, "fallback");
                        break;
                }
            }
        } finally {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpResponse response = client.execute(new HttpGet(url.toExternalForm() + "rest/tracer/reset"));
                Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testHelloAsync() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url.toExternalForm() + "rest/helloAsync"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Hello from async fallback", content);
            response = client.execute(new HttpGet(url.toExternalForm() + "rest/tracer/spans"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            StringTokenizer tokenizer = new StringTokenizer(EntityUtils.toString(response.getEntity()), ";");
            Assert.assertEquals(4, tokenizer.countTokens());
            List<JsonObject> spans = new ArrayList<>(4);
            while (tokenizer.hasMoreTokens()) {
                String jsonContent = tokenizer.nextToken();
                try (JsonReader jsonReader = Json.createReader(new StringReader(jsonContent))) {
                    spans.add(jsonReader.readObject());
                }
            }
            int parentId = 0;
            Collections.sort(spans, (o1, o2) -> {
                return Integer.parseInt(o1.getString("spanId")) - Integer.parseInt(o2.getString("spanId"));
            });
            for (int i = 0; i < spans.size(); i++) {
                JsonObject object = spans.get(i);
                String operationName = object.getString("operationName");
                Assert.assertNotNull(object.toString(), object.getString("traceId"));
                Assert.assertNotNull(object.toString(), object.getString("spanId"));
                Assert.assertEquals(object.toString(), parentId, Integer.parseInt(object.getString("parentId")));
                switch (i) {
                    case 0: {
                        Assert.assertEquals(object.toString(), operationName, "GET:" + HelloResource.class.getCanonicalName() + ".getAsync");
                        parentId = Integer.parseInt(object.getString("spanId"));
                        break;
                    }
                    case 1: {
                        Assert.assertEquals(object.toString(), operationName, "helloAsync");
                        JsonObject log = object.getJsonObject("logs");
                        Assert.assertNotNull(object.toString(), log);
                        String event = log.getString("event");
                        Assert.assertTrue(event, "attempt 0".equals(event));
                        break;
                    }
                    case 2: {
                        Assert.assertEquals(object.toString(), operationName, "helloAsync");
                        JsonObject log = object.getJsonObject("logs");
                        Assert.assertNotNull(object.toString(), log);
                        String event = log.getString("event");
                        Assert.assertTrue(event, "attempt 1".equals(event));
                        break;
                    }
                    case 3:
                        Assert.assertEquals(object.toString(), operationName, "fallbackAsync");
                        JsonObject log = object.getJsonObject("logs");
                        Assert.assertNotNull(object.toString(), log);
                        String event = log.getString("event");
                        Assert.assertTrue(event, "fallbackAsync 2".equals(event));
                        break;
                }
            }
        } finally {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpResponse response = client.execute(new HttpGet(url.toExternalForm() + "rest/tracer/reset"));
                Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }
}
