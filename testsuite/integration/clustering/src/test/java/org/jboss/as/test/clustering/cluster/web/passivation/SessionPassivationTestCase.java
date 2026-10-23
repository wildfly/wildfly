/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.passivation;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Test;

@ServerSetup(EnableUndertowStatisticsSetupTask.class)
public abstract class SessionPassivationTestCase extends AbstractClusteringTestCase {

    private static final Duration MAX_PASSIVATION_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(10));
    private static final Duration PASSIVATION_WAIT_DURATION = MAX_PASSIVATION_DURATION.dividedBy(100);

    private static final String ATTRIBUTE_NAME = "a";

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class, PassivationEventTrackerUtil.class);
        // Take web.xml from the managed test.
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL)
            throws Exception {

        String session1;
        String session2;

        try (CloseableHttpClient client1 = TestHttpClientUtils.promiscuousCookieHttpClient();
             CloseableHttpClient client2 = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try {
                // This should not trigger any passivation/activation events
                try (CloseableHttpResponse response = client1.execute(new HttpPut(SessionOperationServlet.createURI(baseURL, ATTRIBUTE_NAME, "1")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    session1 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
                }

                // Avoid passivating cache entries before they are committed
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                Map<String, Queue<PassivationEventTrackerUtil.EventType>> events = new HashMap<>();
                Map<String, PassivationEventTrackerUtil.EventType> expectedEvents = new HashMap<>();
                events.put(session1, new LinkedList<>());
                expectedEvents.put(session1, PassivationEventTrackerUtil.EventType.PASSIVATION);

                // This will trigger passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpPut(SessionOperationServlet.createURI(baseURL, ATTRIBUTE_NAME, "2")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    session2 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
                    events.put(session2, new LinkedList<>());
                    expectedEvents.put(session2, PassivationEventTrackerUtil.EventType.PASSIVATION);
                    collectEvents(response, events);
                }

                // Ensure session1 was passivated
                awaitEvents(client2, baseURL, session2, "2", session1, events);
                validateEvents(session1, events, expectedEvents);

                // Avoid passivating cache entries before they are committed.
                // Additionally, HttpSessionActivationListener.sessionWillPassivate(...) is triggered at the start of the eviction
                // of a cache entry - consequently session1 may not yet have been evicted from memory, in which case the request
                // below would not trigger its activation.
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                // This should trigger activation of session1 and passivation of session2
                try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, ATTRIBUTE_NAME)))) {
                    validateResponse(response, session1, "1");
                    collectEvents(response, events);
                }

                // Ensure session1 was activated
                awaitEvents(client1, baseURL, session1, "1", session1, events);
                validateEvents(session1, events, expectedEvents);

                // Ensure session2 was passivated
                awaitEvents(client1, baseURL, session1, "1", session2, events);
                validateEvents(session2, events, expectedEvents);

                // Ditto, this time for the passivation of session2
                Thread.sleep(GRACE_TIME_TO_REPLICATE);

                // This should trigger activation of session2 and passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, ATTRIBUTE_NAME)))) {
                    validateResponse(response, session2, "2");
                    collectEvents(response, events);
                }

                // Ensure session2 was activated
                awaitEvents(client2, baseURL, session2, "2", session2, events);
                validateEvents(session2, events, expectedEvents);

                // Ensure session1 was passivated
                awaitEvents(client2, baseURL, session2, "2", session1, events);
                validateEvents(session1, events, expectedEvents);
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

    /**
     * Reads the session associated with the specified client - collecting any reported passivation/activation events - until an event
     * was reported for the session identified by {@code sessionId}, or until {@link #MAX_PASSIVATION_DURATION} has elapsed.
     * Passivation of a cache entry happens asynchronously with respect to the request that triggered it, thus the associated event is
     * not necessarily reported by the response of that request.
     * @param client a client whose session is currently active, i.e. the session used to poll the server for events
     * @param baseURL the base URL of the deployment
     * @param activeSessionId the identifier of the session associated with the specified client
     * @param activeSessionValue the value of the session attribute of the session associated with the specified client
     * @param sessionId the identifier of the session whose event we are waiting for
     * @param events the per session event queues, populated by this method
     */
    private static void awaitEvents(CloseableHttpClient client, URL baseURL, String activeSessionId, String activeSessionValue, String sessionId, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events) throws Exception {
        Instant start = Instant.now();
        while (events.get(sessionId).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, ATTRIBUTE_NAME)))) {
                validateResponse(response, activeSessionId, activeSessionValue);
                collectEvents(response, events);
            }
            Thread.sleep(PASSIVATION_WAIT_DURATION.toMillis());
        }

        assertFalse(events.get(sessionId).isEmpty(), () -> String.format("No passivation/activation event was reported for session %s within %s", sessionId, MAX_PASSIVATION_DURATION));
    }

    private static void validateResponse(HttpResponse response, String sessionId, String value) {
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(sessionId, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
        assertEquals(value, getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
    }

    private static void collectEvents(HttpResponse response, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events) {
        events.entrySet().forEach((Map.Entry<String, Queue<PassivationEventTrackerUtil.EventType>> entry) -> {
            String sessionId = entry.getKey();
            if (response.containsHeader(sessionId)) {
                Stream.of(response.getHeaders(sessionId)).forEach((Header header) ->
                    entry.getValue().add(PassivationEventTrackerUtil.EventType.valueOf(header.getValue())));
            }
        });
    }

    private static void validateEvents(String sessionId, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events, Map<String, PassivationEventTrackerUtil.EventType> expectedEvents) {
        Queue<PassivationEventTrackerUtil.EventType> types = events.get(sessionId);
        PassivationEventTrackerUtil.EventType type = types.poll();
        PassivationEventTrackerUtil.EventType expected = expectedEvents.get(sessionId);
        while (type != null) {
            assertSame(expected, type, String.format("Unexpected event reported for session %s", sessionId));
            type = types.poll();
            // ACTIVATE event must follow PASSIVATE event and vice versa
            expected = PassivationEventTrackerUtil.EventType.values()[(expected.ordinal() + 1) % 2];
        }
        expectedEvents.put(sessionId, expected);
    }

    private static String getRequiredHeaderValue(HttpResponse response, String name) {
        assertTrue(response.containsHeader(name), String.format("response doesn't contain header '%s', all response headers = %s", name, showHeaders(response.getAllHeaders())));
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
