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
package org.jboss.as.test.clustering.cluster.web.expiration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.as.test.clustering.cluster.web.EnableUndertowStatisticsSetupTask;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates get/set/remove/invalidate session operations, session expiration, and their corresponding events
 *
 * @author Paul Ferraro
 */
@ServerSetup(EnableUndertowStatisticsSetupTask.class)
@RunWith(Arquillian.class)
public abstract class SessionExpirationTestCase extends AbstractClusteringTestCase {

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
                             throws IOException, URISyntaxException, InterruptedException {

        HttpResponse response;
        String sessionId;

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            // This should trigger session creation event, but not added attribute event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a")));
            try {
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals(response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionOperationServlet.CREATED_SESSIONS).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This should trigger attribute added event and bound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "1")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                Assert.assertEquals("1", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("1", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Make sure remove attribute event is not fired since attribute does not exist
            response = client.execute(new HttpGet(SessionOperationServlet.createRemoveURI(baseURL2, "b")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute replaced event, as well as valueBound/valueUnbound binding events
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "2")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.REPLACED_ATTRIBUTES).getValue());
                Assert.assertEquals("2", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
                Assert.assertEquals("1", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("2", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute removed event and valueUnbound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                Assert.assertEquals("2", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute added event and valueBound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL1, "a", "3", "4")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                Assert.assertEquals("3", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("4", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute removed event and valueUnbound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createRemoveURI(baseURL1, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                Assert.assertEquals("4", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // No events should have been triggered on remote node
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This should trigger attribute added event and valueBound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL2, "a", "5")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                Assert.assertEquals("5", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger session destroyed event, attribute removed event, and valueUnbound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createInvalidateURI(baseURL1)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals(response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionOperationServlet.DESTROYED_SESSIONS).getValue());
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                Assert.assertEquals("5", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // This should trigger attribute added event and valueBound binding event
            response = client.execute(new HttpGet(SessionOperationServlet.createSetURI(baseURL2, "a", "6")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                Assert.assertEquals(response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue(), response.getFirstHeader(SessionOperationServlet.CREATED_SESSIONS).getValue());
                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.ADDED_ATTRIBUTES).getValue());
                Assert.assertEquals("6", response.getFirstHeader(SessionOperationServlet.BOUND_ATTRIBUTES).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This should not trigger any events
            response = client.execute(new HttpGet(SessionOperationServlet.createGetAndSetURI(baseURL2, "a", "7")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("6", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // This should not trigger any events
            response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL2, "a")));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertEquals("7", response.getFirstHeader(SessionOperationServlet.RESULT).getValue());
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            if (!this.transactional) {
                Thread.sleep(AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE);
            }

            // Trigger session timeout in 1 second
            response = client.execute(new HttpGet(SessionOperationServlet.createTimeoutURI(baseURL1, 1)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                Assert.assertFalse(response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                sessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }

        // Trigger timeout of sessionId - accounts for session timeout (1s) and infinispan reaper thread interval (1s)
        // so that test conditions are typically met within the first attempt
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        // Use a new session for awaiting expiration notification of the previous session scheduled to expire
        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            boolean destroyed = false;
            String newSessionId = null;
            int maxAttempts = 30;

            // Retry a couple of times since expiration in the remote case expiration depends on timing of the reaper thread
            for (int attempt = 1; attempt <= maxAttempts && !destroyed; attempt++) {

                // Timeout should trigger session destroyed event, attribute removed event, and valueUnbound binding event
                for (URL baseURL : Arrays.asList(baseURL1, baseURL2)) {
                    if (!destroyed) {
                        response = client.execute(new HttpGet(SessionOperationServlet.createGetURI(baseURL, "a", sessionId)));
                        try {
                            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                            Assert.assertFalse(response.containsHeader(SessionOperationServlet.RESULT));
                            Assert.assertTrue(response.containsHeader(SessionOperationServlet.SESSION_ID));
                            Assert.assertEquals(newSessionId == null, response.containsHeader(SessionOperationServlet.CREATED_SESSIONS));
                            if (newSessionId == null) {
                                newSessionId = response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue();
                            } else {
                                Assert.assertEquals(newSessionId, response.getFirstHeader(SessionOperationServlet.SESSION_ID).getValue());
                            }
                            destroyed = response.containsHeader(SessionOperationServlet.DESTROYED_SESSIONS);
                            Assert.assertFalse(response.containsHeader(SessionOperationServlet.ADDED_ATTRIBUTES));
                            Assert.assertFalse(response.containsHeader(SessionOperationServlet.REPLACED_ATTRIBUTES));
                            Assert.assertEquals(destroyed, response.containsHeader(SessionOperationServlet.REMOVED_ATTRIBUTES));
                            Assert.assertFalse(response.containsHeader(SessionOperationServlet.BOUND_ATTRIBUTES));
                            Assert.assertEquals(destroyed, response.containsHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES));
                            if (destroyed) {
                                Assert.assertEquals(sessionId, response.getFirstHeader(SessionOperationServlet.DESTROYED_SESSIONS).getValue());
                                Assert.assertEquals("a", response.getFirstHeader(SessionOperationServlet.REMOVED_ATTRIBUTES).getValue());
                                Assert.assertEquals("7", response.getFirstHeader(SessionOperationServlet.UNBOUND_ATTRIBUTES).getValue());
                                log.infof("Session destroyed within %d attempts.", attempt);
                            }
                        } finally {
                            HttpClientUtils.closeQuietly(response);
                        }
                    }
                }

                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            }
            Assert.assertTrue("Session has not been destroyed following expiration within " + maxAttempts + " attempts.", destroyed);
        }
    }
}
