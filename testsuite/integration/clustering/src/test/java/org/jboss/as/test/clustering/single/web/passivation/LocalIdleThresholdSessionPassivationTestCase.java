/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.awaitility.Awaitility;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.PassivationEventTrackerUtil;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Validates the correctness of session activation/passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction.
 *
 * @author Radoslav Husar
 */
public abstract class LocalIdleThresholdSessionPassivationTestCase {

    // Max idle time configured in jboss-web-idle.xml is PT3S (3 seconds)
    // Maximum passivation polling duration after which the test will abort waiting for the passivation event
    // This needs to be longer than idle-threshold (3s) to allow time for passivation to occur
    private static final Duration PASSIVATION_POLLING_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(15));
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class, PassivationEventTrackerUtil.class);
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) URL baseURL) throws IOException, URISyntaxException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String sessionId;

            // Step 1: Create a session and set an attribute
            // This should not trigger any passivation/activation events
            try (CloseableHttpResponse response = client.execute(new HttpPut(SessionOperationServlet.createURI(baseURL, "testAttr", "testValue")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                sessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
            }

            Map<String, Queue<PassivationEventTrackerUtil.EventType>> events = new HashMap<>();
            events.put(sessionId, new LinkedList<>());

            // Step 2: Wait for idle timeout - session should be passivated
            // but actually do NOT wait – keep polling for the event; this makes the test faster and more resilient as opposed to time-based approach

            // Step 3: Poll to verify session was passivated due to idle timeout
            // Use HEAD request with a NEW client (no session cookie) to check events without resetting the idle timer
            String finalSessionId = sessionId;
            Awaitility.await("session to passivate")
                    .atMost(PASSIVATION_POLLING_DURATION)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try (CloseableHttpClient pollingClient = HttpClients.createDefault();
                             CloseableHttpResponse response = pollingClient.execute(new HttpHead(SessionOperationServlet.createURI(baseURL)))) {
                            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                            collectEvents(response, events);
                            return !events.get(finalSessionId).isEmpty();
                        }
                    });

            assertFalse("Session should have been passivated after idle timeout", events.get(sessionId).isEmpty());
            assertEquals("First event should be PASSIVATION", PassivationEventTrackerUtil.EventType.PASSIVATION, events.get(sessionId).peek());

            // Step 4: Access the session again - should trigger activation
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "testAttr")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("testValue", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                collectEvents(response, events);
            }

            // Verify activation occurred
            assertTrue("Session should have been activated", events.get(sessionId).contains(PassivationEventTrackerUtil.EventType.ACTIVATION));
            validateEvents(sessionId, events);

            // Step 5: Test a second idle cycle
            // Clear events for the second cycle
            events.get(sessionId).clear();

            // Step 6: Wait for second idle timeout - session should be passivated
            // again - don't wait, but keep polling with a new client (no session cookie)
            Awaitility.await("session to passivate again")
                    .atMost(PASSIVATION_POLLING_DURATION)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try (CloseableHttpClient pollingClient = HttpClients.createDefault();
                             CloseableHttpResponse response = pollingClient.execute(new HttpHead(SessionOperationServlet.createURI(baseURL)))) {
                            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                            collectEvents(response, events);
                            return !events.get(finalSessionId).isEmpty();
                        }
                    });

            assertFalse("Session should have been passivated again after second idle timeout", events.get(sessionId).isEmpty());
            assertEquals("First event of second cycle should be PASSIVATION", PassivationEventTrackerUtil.EventType.PASSIVATION, events.get(sessionId).peek());

            // Step 7: Access the session again - should trigger second activation
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createURI(baseURL, "testAttr")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("testValue", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                collectEvents(response, events);
            }

            // Verify second activation occurred
            assertTrue("Session should have been activated again", events.get(sessionId).contains(PassivationEventTrackerUtil.EventType.ACTIVATION));
            validateEvents(sessionId, events);

            // Cleanup
            try (CloseableHttpResponse response = client.execute(new HttpDelete(SessionOperationServlet.createURI(baseURL)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }
        }
    }

    private static void collectEvents(HttpResponse response, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events) {
        events.forEach((sessionId, value) -> {
            if (response.containsHeader(sessionId)) {
                Stream.of(response.getHeaders(sessionId)).forEach((Header header) -> {
                    value.add(PassivationEventTrackerUtil.EventType.valueOf(header.getValue()));
                });
            }
        });
    }

    private static void validateEvents(String sessionId, Map<String, Queue<PassivationEventTrackerUtil.EventType>> events) {
        Queue<PassivationEventTrackerUtil.EventType> types = events.get(sessionId);
        PassivationEventTrackerUtil.EventType expected = PassivationEventTrackerUtil.EventType.PASSIVATION;

        for (PassivationEventTrackerUtil.EventType type : types) {
            assertEquals("Events should alternate between PASSIVATION and ACTIVATION", expected, type);
            // ACTIVATION event must follow PASSIVATION event and vice versa
            expected = PassivationEventTrackerUtil.EventType.values()[(expected.ordinal() + 1) % 2];
        }
    }
}
