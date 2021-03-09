/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.single.web.passivation;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Validates the correctness of session activation/passivation events for a distributed session manager using a local, passivating cache.
 * @author Paul Ferraro
 */
public abstract class LocalSessionPassivationTestCase {

    private static final Duration MAX_PASSIVATION_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(10));

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class);
        war.addAsWebInfResource(LocalSessionPassivationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL) throws IOException, URISyntaxException {

        try (CloseableHttpClient client1 = HttpClients.createDefault()) {
            try (CloseableHttpClient client2 = HttpClients.createDefault()) {

                String session1 = null;

                // This should not trigger any passivation/activation events
                try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL, "a", "1")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                    session1 = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
                }

                Map<String, Queue<SessionOperationServlet.EventType>> events = new HashMap<>();
                Map<String, SessionOperationServlet.EventType> expectedEvents = new HashMap<>();
                events.put(session1, new LinkedList<>());
                expectedEvents.put(session1, SessionOperationServlet.EventType.PASSIVATION);

                Instant start = Instant.now();
                String session2 = null;

                // This will trigger passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL, "a", "2")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                    session2 = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
                    events.put(session2, new LinkedList<>());
                    expectedEvents.put(session2, SessionOperationServlet.EventType.PASSIVATION);
                    collectEvents(response, events);
                }

                // Ensure session1 was passivated
                while (events.get(session1).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                        assertEquals(session2, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                        assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                        assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                        collectEvents(response, events);
                    }
                    Thread.yield();
                }

                assertFalse(events.get(session1).isEmpty());
                validateEvents(session1, events, expectedEvents);

                start = Instant.now();

                // This should trigger activation of session1 and passivation of session2
                try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                    assertEquals(session1, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                    assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                    assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                    collectEvents(response, events);
                    assertFalse(events.get(session1).isEmpty());
                    assertTrue(events.get(session1).contains(SessionOperationServlet.EventType.ACTIVATION));
                }

                // Verify session2 was passivated
                while (events.get(session2).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                        assertEquals(session1, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                        assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                        assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                        collectEvents(response, events);
                    }
                    Thread.yield();
                }

                assertFalse(events.get(session2).isEmpty());
                validateEvents(session2, events, expectedEvents);

                start = Instant.now();

                // This should trigger activation of session2 and passivation of session1
                try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                    assertEquals(session2, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                    assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                    assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                    collectEvents(response, events);
                    assertFalse(events.get(session2).isEmpty());
                    assertTrue(events.get(session2).contains(SessionOperationServlet.EventType.ACTIVATION));
                }

                // Verify session1 was passivated
                while (!events.get(session1).isEmpty() && Duration.between(start, Instant.now()).compareTo(MAX_PASSIVATION_DURATION) < 0) {
                    try (CloseableHttpResponse response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                        assertEquals(session2, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                        assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                        assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                        collectEvents(response, events);
                    }
                    Thread.yield();
                }

                assertFalse(events.get(session1).isEmpty());
                validateEvents(session1, events, expectedEvents);

                validateEvents(session2, events, expectedEvents);
            }
        }
    }

    private static void collectEvents(HttpResponse response, Map<String, Queue<SessionOperationServlet.EventType>> events) {
        events.entrySet().forEach((Map.Entry<String, Queue<SessionOperationServlet.EventType>> entry) -> {
            String sessionId = entry.getKey();
            if (response.containsHeader(sessionId)) {
                Stream.of(response.getHeaders(sessionId)).forEach((Header header) -> {
                    entry.getValue().add(SessionOperationServlet.EventType.valueOf(header.getValue()));
                });
            }
        });
    }

    private static void validateEvents(String sessionId, Map<String, Queue<SessionOperationServlet.EventType>> events, Map<String, SessionOperationServlet.EventType> expectedEvents) {
        Queue<SessionOperationServlet.EventType> types = events.get(sessionId);
        SessionOperationServlet.EventType type = types.poll();
        SessionOperationServlet.EventType expected = expectedEvents.get(sessionId);
        while (type != null) {
            assertSame(expected, type);
            type = types.poll();
            // ACTIVATE event must follow PASSIVATE event and vice versa
            expected = SessionOperationServlet.EventType.values()[(expected.ordinal() + 1) % 2];
        }
        expectedEvents.put(sessionId, expected);
    }
}
