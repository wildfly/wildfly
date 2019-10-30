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

package org.jboss.as.test.clustering.cluster.web.passivation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

public abstract class SessionPassivationTestCase extends AbstractClusteringTestCase {

    private static final int MAX_PASSIVATION_WAIT = TimeoutUtil.adjust(10000);

    static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class);
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
            // This should not trigger any passivation/activation events
            HttpResponse response = client1.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL, "a", "1")));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                session1 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            long now = System.currentTimeMillis();
            long start = now;
            Map<String, Queue<SessionOperationServlet.EventType>> events = new HashMap<>();
            Map<String, SessionOperationServlet.EventType> expectedEvents = new HashMap<>();
            events.put(session1, new LinkedList<>());
            expectedEvents.put(session1, SessionOperationServlet.EventType.PASSIVATION);

            // This will trigger passivation of session1
            response = client2.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL, "a", "2")));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                session2 = getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID);
                events.put(session2, new LinkedList<>());
                expectedEvents.put(session2, SessionOperationServlet.EventType.PASSIVATION);
                collectEvents(response, events);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Ensure session1 was passivated
            while (events.get(session1).isEmpty() && ((now - start) < MAX_PASSIVATION_WAIT)) {
                response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")));
                try {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                    assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                    collectEvents(response, events);
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
                Thread.yield();
                now = System.currentTimeMillis();
            }

            assertFalse(events.get(session1).isEmpty());
            validateEvents(session1, events, expectedEvents);

            now = System.currentTimeMillis();
            start = now;

            // This should trigger activation of session1 and passivation of session2
            response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(session1, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                assertEquals("1", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                collectEvents(response, events);
                assertFalse(events.get(session1).isEmpty());
                assertTrue(events.get(session1).contains(SessionOperationServlet.EventType.ACTIVATION));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Verify session2 was passivated
            while (events.get(session2).isEmpty() && ((now - start) < MAX_PASSIVATION_WAIT)) {
                response = client1.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")));
                try {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertEquals(session1, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                    assertEquals("1", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                    collectEvents(response, events);
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
                Thread.yield();
                now = System.currentTimeMillis();
            }

            assertFalse(events.get(session2).isEmpty());
            validateEvents(session2, events, expectedEvents);

            now = System.currentTimeMillis();
            start = now;

            // This should trigger activation of session2 and passivation of session1
            response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                collectEvents(response, events);
                assertFalse(events.get(session2).isEmpty());
                assertTrue(events.get(session2).contains(SessionOperationServlet.EventType.ACTIVATION));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Verify session1 was passivated
            while (!events.get(session1).isEmpty() && ((now - start) < MAX_PASSIVATION_WAIT)) {
                response = client2.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a")));
                try {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertEquals(session2, getRequiredHeaderValue(response, SessionOperationServlet.SESSION_ID));
                    assertEquals("2", getRequiredHeaderValue(response, SessionOperationServlet.RESULT));
                    collectEvents(response, events);
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
                Thread.yield();
                now = System.currentTimeMillis();
            }

            assertFalse(events.get(session1).isEmpty());
            validateEvents(session1, events, expectedEvents);

            validateEvents(session2, events, expectedEvents);
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
