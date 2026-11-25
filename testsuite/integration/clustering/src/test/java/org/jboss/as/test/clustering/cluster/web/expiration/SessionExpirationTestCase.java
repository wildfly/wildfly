/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web.expiration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.as.test.clustering.cluster.web.EnableUndertowStatisticsSetupTask;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates get/set/remove/invalidate session operations, session expiration, and their corresponding events
 *
 * @author Paul Ferraro
 */
@ServerSetup(EnableUndertowStatisticsSetupTask.class)
@ExtendWith(ArquillianExtension.class)
public abstract class SessionExpirationTestCase extends AbstractClusteringTestCase {

    private static final Duration EXPIRATION_DURATION = Duration.ofSeconds(2);

    public static WebArchive getBaseDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        war.addClasses(SessionOperationServlet.class, RecordingWebListener.class);
        // Take web.xml from the managed test.
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        return war;
    }

    private final boolean transactional;

    protected SessionExpirationTestCase(TransactionMode mode) {
        this.transactional = mode.isTransactional();
    }

    @Test
    public void test(@ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                     @ArquillianResource(SessionOperationServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
                             throws Exception {
        String sessionId;

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            // This should trigger session creation event, but not added attribute event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a")))) {
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                sessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
                assertTrue(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.CREATED_SESSIONS).getValue());
            }

            // This should trigger attribute added event and bound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "1")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                assertEquals("1", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            // Make sure remove attribute event is not fired since attribute does not exist
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createRemoveURI(baseURL2, "b")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute replaced event, as well as valueBound/valueUnbound binding events
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "2")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.REPLACED_ATTRIBUTES).getValue());
                assertEquals("2", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
                assertEquals("1", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute removed event and valueUnbound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                assertEquals("2", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute added event and valueBound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "3", "4")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                assertEquals("3", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("4", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute removed event and valueUnbound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createRemoveURI(baseURL1, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                assertEquals("4", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            // This should trigger attribute added event and valueBound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL2, "a", "5")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                assertEquals("5", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger session destroyed event, attribute removed event, and valueUnbound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createInvalidateURI(baseURL1)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertTrue(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals(response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionOperationServlet.DESTROYED_SESSIONS).getValue());
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                assertEquals("5", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute added event and valueBound binding event
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL2, "a", "6")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                String previousSessionId = sessionId;
                sessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
                assertNotEquals(previousSessionId, sessionId);
                assertTrue(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                assertEquals(response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionOperationServlet.CREATED_SESSIONS).getValue());
                assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                assertEquals("6", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            }

            // This should not trigger any events
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetAndSetURI(baseURL2, "a", "7")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("6", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            // This should not trigger any events
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                assertEquals("7", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // Trigger session timeout in 1 second
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createTimeoutURI(baseURL1, 1)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                sessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
            }

            Instant start = Instant.now();
            // Verify that the session does not expire until it is sufficiently idle
            while (Instant.now().isBefore(start.plus(EXPIRATION_DURATION))) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL1, "a")))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                    assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                }
                Thread.sleep(100);
            }
        }

        // Trigger timeout of sessionId - accounts for session timeout (1s) and infinispan reaper thread interval (1s)
        // so that test conditions are typically met within the first attempt
        Thread.sleep(EXPIRATION_DURATION.toMillis());

        // Use a new session for awaiting expiration notification of the previous session scheduled to expire
        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            boolean destroyed = false;
            boolean removed = false;
            boolean unbound = false;
            int maxAttempts = 30;

            // Once we observe an event on a given node, only poll that node for the remaining events
            URL eventURL = null;

            // Retry a couple of times since in the remote case expiration depends on timing of the reaper thread
            for (int attempt = 1; attempt <= maxAttempts && !(destroyed && removed && unbound); attempt++) {

                // Timeout should trigger session destroyed event, attribute removed event, and valueUnbound binding event
                for (URL baseURL : (eventURL != null) ? List.of(eventURL) : List.of(baseURL1, baseURL2)) {
                    try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a", sessionId)))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                        assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                        assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES), "Unexpected attribute added event");
                        assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES), "Unexpected attribute replaced event");
                        assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES), "Unexpected attribute bound event");

                        boolean hasDestroyed = response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS);
                        boolean hasRemoved = response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES);
                        boolean hasUnbound = response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES);

                        assertFalse(destroyed && hasDestroyed, "Session destroyed event received more than once");
                        assertFalse(removed && hasRemoved, "Attribute removed event received more than once");
                        assertFalse(unbound && hasUnbound, "Attribute unbound event received more than once");

                        if (hasDestroyed) {
                            assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.DESTROYED_SESSIONS).getValue());
                            destroyed = true;
                        }
                        if (hasRemoved) {
                            assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                            removed = true;
                        }
                        if (hasUnbound) {
                            assertEquals("7", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
                            unbound = true;
                        }

                        if (hasDestroyed || hasRemoved || hasUnbound) {
                            eventURL = baseURL;
                            if (destroyed && removed && unbound) {
                                log.infof("Observed destroyed, removed, and unbound events within %d attempts.", attempt);
                            }
                            break;
                        }
                    }
                }

                Thread.sleep(TimeUnit.SECONDS.toMillis(TimeoutUtil.adjust(1)));
            }
            assertTrue(destroyed, "Session has not been destroyed following expiration within %d attempts.".formatted(maxAttempts));
            assertTrue(removed, "Session attribute has not been removed following expiration within %d attempts.".formatted(maxAttempts));
            assertTrue(unbound, "Session attribute has not been unbound following expiration within %d attempts.".formatted(maxAttempts));

            // Verify no expiration events were received on the other node
            URL otherURL = eventURL.equals(baseURL1) ? baseURL2 : baseURL1;
            try (CloseableHttpResponse response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(otherURL, "a", sessionId)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS), "Session destroyed event received on both nodes");
                assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES), "Attribute removed event received on both nodes");
                assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES), "Attribute unbound event received on both nodes");
                assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES), "Unexpected attribute added event on other node");
                assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES), "Unexpected attribute replaced event on other node");
                assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES), "Unexpected attribute bound event on other node");
            }
        }
    }
}
