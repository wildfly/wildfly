/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.app.TimerInfo;
import org.jboss.as.test.clustering.cluster.ejb.timer.app.TimerRecord;
import org.jboss.as.test.clustering.cluster.ejb.timer.app.TimerServletEndpoint;
import org.jboss.as.test.clustering.cluster.ejb.timer.app.TimerWSEndpoint;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientWebSocket;
import io.vertx.core.http.WebSocketClient;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletResponse;

@RunWith(Arquillian.class)
public class DistributedTimerAccessTestCase extends AbstractClusteringTestCase {

    private static final Logger log = Logger.getLogger(DistributedTimerAccessTestCase.class);

    public static final String DEPLOYMENT = "TimerApp";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    protected static WebArchive createArchive() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addPackage(TimerServletEndpoint.class.getPackage());
    }

    class ServerEvent implements Handler<String> {

        private List<TimerRecord> records;
        private CountDownLatch latch;

        public ServerEvent(CountDownLatch latch) {
            this.records = Collections.synchronizedList(new ArrayList<>());
            this.latch = latch;
        }

        public void reset(CountDownLatch latch) {
            this.latch = latch;
            records.clear();
        }

        public int size() {
            return records.size();
        }

        @Override
        public void handle(String event) {
            log.infof("received from server event %s", event);
            JsonObject jsonObject = Json.createReader(new StringReader(event)).readObject();
            String nodeName = jsonObject.getString("nodeName");
            String className = jsonObject.getString("className");
            TimerInfo info = toTimerInfo(jsonObject.getJsonObject("info"));
            Boolean persistent = jsonObject.getBoolean("persistent");
            Instant now = Instant.parse(jsonObject.getString("now"));
            records.add(new TimerRecord(nodeName, className, info, persistent, now));
            latch.countDown();
        }

    }

    private WebSocketClient newWebSocket(URI node, Handler<String> textHandler) {
        URI endpoint = node.resolve(TimerWSEndpoint.ENDPOINT);
        WebSocketClient client = Vertx.vertx().createWebSocketClient();
        ClientWebSocket webSocket = client.webSocket();
        webSocket.textMessageHandler(textHandler);
        log.infof("connecting to %s", endpoint.toString());
        webSocket.connect(node.getPort(), node.getHost(), endpoint.getPath());
        log.infof("connected to %s", endpoint.toString());
        return client;
    }

    @Test
    public void testDistributeCacheTimerAccess(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URI node1URL,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URI node2URL)
            throws InterruptedException, URISyntaxException {

        final int numberOfTimers = 5;
        List<URI> httpEndpoints = new ArrayList<>();
        List<WebSocketClient> wsClients = new ArrayList<>();

        try (CloseableHttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            List<URI> uris = List.of(node1URL, node2URL);

            CountDownLatch latch = new CountDownLatch(numberOfTimers * uris.size());
            ServerEvent events = new ServerEvent(latch);

            uris.stream().map(uri -> uri.resolve(TimerServletEndpoint.ENDPOINT)).forEach(httpEndpoints::add);
            uris.stream().map(url -> newWebSocket(url, events)).forEach(wsClients::add);

            // test normal execution in timeout guard
            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                for (int i = 0; i < numberOfTimers; i++) {
                    boolean isTimerCreated = createTimer(httpClient, endpoint, String.valueOf(j * numberOfTimers + i),
                            (i + 1) * 1000L, false, false);
                    assertTrue("uri not getting status " + endpoint, isTimerCreated);
                }
            }

            assertTrue(latch.await(20, TimeUnit.SECONDS));

            // we test preDestroy iteration
            // two different timers 1 will be destroyed by pre destroy and the other not so we can verify this will happen
            latch = new CountDownLatch(uris.size());
            events.reset(latch);
            wsClients.forEach(WebSocketClient::close);

            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                boolean isTimerCreated = createTimer(httpClient, endpoint, "preDestroy-shouldnotbetriggered-" + j, 4000L, true, false);
                assertTrue("uri not getting status " + endpoint, isTimerCreated);
                isTimerCreated = createTimer(httpClient, endpoint, "preDestroy-" + j, 4000L, false, false);
                assertTrue("uri not getting status " + endpoint, isTimerCreated);
            }

            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                List<TimerInfo> timers = getTimers(httpClient, endpoint);
                assertEquals(2*uris.size(), timers.size());
            }

            // this is going to force call preDestroy and therefore cancel both timers
            this.stop(NODE_1_2);
            this.start(NODE_1_2);
            uris.stream().map(url -> newWebSocket(url, events)).forEach(wsClients::add);

            // we ensure we have at least one event per URI
            assertTrue(latch.await(20, TimeUnit.SECONDS));
            assertEquals(uris.size(), events.size());
            // there should not be any other events left
            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                List<TimerInfo> timers = getTimers(httpClient, endpoint);
                assertEquals(0, timers.size());
            }

            // two different timers 1 will be destroyed by pre destroy and the other not so we can verify this will happen
            // just one event and the other one was canceled
            latch = new CountDownLatch(uris.size());
            events.reset(latch);
            wsClients.forEach(WebSocketClient::close);

            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                boolean isTimerCreated = createTimer(httpClient, endpoint, "postConstruct-shouldnotbetriggered-" + j, 4000L, false, true);
                assertTrue("uri not getting status " + endpoint, isTimerCreated);
                isTimerCreated = createTimer(httpClient, endpoint, "postConstruct-" + j, 4000L, false, false);
                assertTrue("uri not getting status " + endpoint, isTimerCreated);
            }

            // we verify that every member of the cluster sees 4 timers (2 per node
            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                List<TimerInfo> timers = getTimers(httpClient, endpoint);
                assertEquals(2*uris.size(), timers.size());
            }

            this.stop(NODE_1_2);
            this.start(NODE_1_2);
            uris.stream().map(url -> newWebSocket(url, events)).forEach(wsClients::add);

            // we ensure we have at least one event per URI
            assertTrue(latch.await(20, TimeUnit.SECONDS));
            assertEquals(uris.size(), events.size());
            // there should not be any other events left
            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                List<TimerInfo> timers = getTimers(httpClient, endpoint);
                assertEquals(0, timers.size());
            }

        } catch (IOException e) {
            fail("an exception happend", e);
        } finally {
            wsClients.forEach(WebSocketClient::close);
        }

    }

    private List<TimerInfo> getTimers(CloseableHttpClient httpClient, URI uri) throws ClientProtocolException, IOException {
        List<TimerInfo> timers = new ArrayList<>();
        HttpGet get = new HttpGet(uri);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (HttpServletResponse.SC_OK != response.getStatusLine().getStatusCode()) {
                throw new RuntimeException("invalid reponse from server while getting timers");
            }
            byte[] content = response.getEntity().getContent().readAllBytes();
            JsonArray jsonArray = Json.createReader(new StringReader(new String(content))).readArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                timers.add(toTimerInfo(jsonArray.getJsonObject(i)));
            }
        }
        return timers;
    }

    private boolean createTimer(CloseableHttpClient httpClient, URI uri, String info, Long duration, Boolean preDestroy,
            Boolean postConstruct) throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(uri);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("info", info);
        builder.add("duration", duration);
        builder.add("preDestroy", preDestroy);
        builder.add("postConstruct", postConstruct);
        HttpEntity entity = new StringEntity(builder.build().toString(), ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            return HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode();
        }
    }

    private TimerInfo toTimerInfo(JsonObject jsonObject) {
        return new TimerInfo(
                jsonObject.getJsonNumber("duration").longValue(),
                jsonObject.getString("info"),
                jsonObject.getBoolean("preDestroy"),
                jsonObject.getBoolean("postConstruct"));
    }
}
