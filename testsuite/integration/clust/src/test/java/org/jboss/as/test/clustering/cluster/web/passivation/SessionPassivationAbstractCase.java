/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.net.URL;
import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * HTTP Session passivation tests.
 *
 * Tests: max-idle, min-idle, max-active-sessions
 *
 * To be extended with: passivation around fail-over events (restart, shutdown, deploy)
 *
 * @author Radoslav Husar
 * @version April 2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class SessionPassivationAbstractCase {

    // Sync these with jboss-web.xml
    private int MAX_ACTIVE_SESSIONS = 20;
    private int PASSIVATION_MIN_IDLE_TIME = 5;
    private int PASSIVATION_MAX_IDLE_TIME = 10;
    // ARQ 
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(-1)
    public void testStartContainers() {
        NodeUtil.start(controller, deployer, CONTAINER_1, DEPLOYMENT_1);

        // By forming a cluster it is not possible to have equivalence between 'session was passivated' and the
        // fact 'session was serialized'. A CacheListener for passivation event will be needed. --Rado
        // NodeUtil.start(controller, deployer, CONTAINER_2, DEPLOYMENT_2);
    }

    /**
     * Tests the ability to passivate session when max-idle-time for session is reached.
     *
     * @throws Exception
     */
    @Test
    public void testSessionPassivationWithMaxIdleTime(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {

        // Create an instance of HttpClient
        HttpClient client = HttpClientUtils.relaxedCookieHttpClient();
        try {
            // Setup the session
            HttpResponse response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertFalse("Session should not be serialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            response.getEntity().getContent().close();

            // Get the attribute set
            response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertFalse("Session still should not be serialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            response.getEntity().getContent().close();

            // Sleep up to 20 secs to allow max-idle to be reached and JBoss Web background process to run
            // knowing that max-idle in jboss-web.xml is 10 seconds and tomcat JBoss Web process is using the
            // default 10 seconds and processExpiresFrequency is 1 so kicks in every time.
            Thread.sleep((PASSIVATION_MAX_IDLE_TIME + 10) * 1000);

            // Activate the session by requesti ng the attribute
            response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertTrue("Session should have activated and have been deserialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Test that session is not passivated before minimum idle time (passivation-min-idle-time) is reached.
     *
     * @throws Exception
     */
    @Test
    public void testSessionPassivationWithMinIdleTime(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {

        // Create an instance of HttpClient
        HttpClient client = HttpClientUtils.relaxedCookieHttpClient();

        try {
            // Setup the session
            HttpResponse response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertFalse("Session should not be serialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            response.getEntity().getContent().close();

            // Sleep for less than passivation-min-idle-time
            Thread.sleep(PASSIVATION_MIN_IDLE_TIME * 1000 - 100);

            // Make sure the session hasn't passivated already
            response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertFalse("Session should not have been passivated",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Tests the ability to passivate session when maximum number of active sessions (max-active-sessions) reached.
     *
     * @throws Exception
     */
    @Test
    @Ignore("https://issues.jboss.org/browse/AS7-4490")
    public void testSessionPassivationWithMaxActiveSessions(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {

        // Create an instance of HttpClient
        HttpClient client = HttpClientUtils.relaxedCookieHttpClient();
        try {
            // Setup the session
            HttpResponse response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            response.getEntity().getContent().close();

            // Get the Attribute set; confirm session wasn't deserialized
            response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertFalse("Session should not be serialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            response.getEntity().getContent().close();

            // passivation-min-idle-time is set to 5 seconds, so wait long enough
            // so that the session can be passivated
            Thread.sleep(PASSIVATION_MIN_IDLE_TIME + 1000);

            // Create enough sessions on the server to trigger passivation
            // knowing that max-active-sessions is set to 20 in jboss-web.xml
            for (int i = 0; i < MAX_ACTIVE_SESSIONS; i++) {
                HttpClient maxActiveClient = new DefaultHttpClient();

                try {
                    response = ClusterHttpClientUtil.tryGet(maxActiveClient, baseURL1 + SimpleServlet.URL);
                    Assert.assertFalse("Session should not be serialized",
                            Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
                    Assert.assertEquals("The session should be new",
                            1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                    response.getEntity().getContent().close();

                    response = ClusterHttpClientUtil.tryGet(maxActiveClient, baseURL1 + SimpleServlet.URL);
                    response.getEntity().getContent().close();
                } finally {
                    maxActiveClient.getConnectionManager().shutdown();
                }
            }

            // Now access the session and confirm that it was deserialized (passivated then activated)
            // Activate the session by requesting the attribute
            response = ClusterHttpClientUtil.tryGet(client, baseURL1 + SimpleServlet.URL);
            Assert.assertTrue("Session should have activated and have been deserialized",
                    Boolean.valueOf(response.getFirstHeader(SimpleServlet.HEADER_SERIALIZED).getValue()));
            Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
