/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.passivation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.PassivationEventTrackerUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.as.test.clustering.cluster.web.EnableUndertowStatisticsSetupTask;
import org.jboss.as.test.clustering.single.web.passivation.SessionOperationServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

@ServerSetup(EnableUndertowStatisticsSetupTask.class)
public abstract class SessionPassivationTestCase extends AbstractClusteringTestCase {

    private static final Duration MAX_PASSIVATION_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(10));
    private static final Duration PASSIVATION_WAIT_DURATION = MAX_PASSIVATION_DURATION.dividedBy(100);

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class, PassivationEventTrackerUtil.class);
        // Take web.xml from the managed test.
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL)
            throws IOException, URISyntaxException {

        String session1;
        String session2;

        try (CloseableHttpClient client1 = TestHttpClientUtils.promiscuousCookieHttpClient();
             CloseableHttpClient client2 = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try {
                // This should not trigger any passivation/activation events
                try (CloseableHttpResponse response = client1.execute(new HttpPut(SessionOperationServlet.createURI(baseURL, "a", "1")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    session1 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
                }

                // Avoid passivating cache entries before they are committed
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                Instant start = Instant.now();
                Map<String, Queue<PassivationEventTrackerUtil.EventType>> events = new HashMap<>();
                Map<String, PassivationEventTrackerUtil.EventType> expectedEvents = new HashMap<>();
                events.put(session1, new LinkedList<>());
                expectedEvents.put(session1, PassivationEventTrackerUtil.EventType.PASSIVATION);

                // This will trigger passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpPut(SessionOperationServlet.createURI(baseURL, "a", "2")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    session2 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
                    events.put(session2, new LinkedList<>());
                    expectedEvents.put(session2, PassivationEventTrackerUtil.EventType.PASSIVATION);
                    collectEvents(response, events);
                }

                // Ensure session1 was passivated
                while (events.get(session1).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                        assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                        collectEvents(response, events);
                    }
                    Thread.sleep(PASSIVATION_WAIT_DURATION.toMillis());
                }

                assertFalse(events.get(session1).isEmpty());
                validateEvents(session1, events, expectedEvents);

                // Avoid passivating cache entries before they are committed
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                start = Instant.now();

                // This should trigger activation of session1 and passivation of session2
                try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "a")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertEquals(session1, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                    assertEquals("1", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                    collectEvents(response, events);
                    assertFalse(events.get(session1).isEmpty());
                    assertTrue(events.get(session1).contains(PassivationEventTrackerUtil.EventType.ACTIVATION));
                }

                // Verify session2 was passivated
                while (events.get(session2).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertEquals(session1, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                        assertEquals("1", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                        collectEvents(response, events);
                    }
                    Thread.sleep(PASSIVATION_WAIT_DURATION.toMillis());
                }

                assertFalse(events.get(session2).isEmpty());
                validateEvents(session2, events, expectedEvents);

                // Avoid passivating cache entries before they are committed
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                start = Instant.now();

                // This should trigger activation of session2 and passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "a")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                    assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                    collectEvents(response, events);
                    assertFalse(events.get(session2).isEmpty());
                    assertTrue(events.get(session2).contains(PassivationEventTrackerUtil.EventType.ACTIVATION));
                }

                // Verify session1 was passivated
                while (!events.get(session1).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                        assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                        collectEvents(response, events);
                    }
                    Thread.sleep(PASSIVATION_WAIT_DURATION.toMillis());
                }

                assertFalse(events.get(session1).isEmpty());
                validateEvents(session1, events, expectedEvents);

                validateEvents(session2, events, expectedEvents);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                for (CloseableHttpClient client : List.of(client1, client2)) {
                    try (CloseableHttpResponse response = client.execute(new HttpDelete(SessionOperationServlet.createURI(baseURL)))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    }
                }
            }
        }
    }

    private static void collectEvents(HttpResponse response, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events) {
        events.entrySet().forEach((Map.Entry<String, Queue<PassivationEventTrackerUtil.EventType>> entry) -> {
            String sessionId = entry.getKey();
            if (response.containsHeader(sessionId)) {
                Stream.of(response.getHeaders(sessionId)).forEach((Header header) -> {
                    entry.getValue().add(PassivationEventTrackerUtil.EventType.valueOf(header.getValue()));
                });
            }
        });
    }

    private static void validateEvents(String sessionId, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events, Map<String, PassivationEventTrackerUtil.EventType> expectedEvents) {
        Queue<PassivationEventTrackerUtil.EventType> types = events.get(sessionId);
        PassivationEventTrackerUtil.EventType type = types.poll();
        PassivationEventTrackerUtil.EventType expected = expectedEvents.get(sessionId);
        while (type != null) {
            assertSame(expected, type);
            type = types.poll();
            // ACTIVATE event must follow PASSIVATE event and vice versa
            expected = PassivationEventTrackerUtil.EventType.values()[(expected.ordinal() + 1) % 2];
        }
        expectedEvents.put(sessionId, expected);
    }

    private static String getRequiredHeaderValue(HttpResponse response, String name) {
        assertTrue(String.format("response doesn't contain header '%s', all response headers = %s", name, showHeaders(response.getAllHeaders())), response.containsHeader(name));
        return response.getFirstHeader(name).getValue();
    }

    private static String showHeaders(Header[] headers) {
        StringBuilder stringBuilder = new StringBuilder();
        try (Formatter result = new Formatter(stringBuilder)) {
            Stream.of(headers).forEach((Header header) -> result.format("{name=%s, value=%s}, ", header.getName(), header.getValue()));
            return result.toString();
        }
    }
}
