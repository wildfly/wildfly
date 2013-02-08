/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that failover and undeploy works.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class ClusteredWebFailoverAbstractCase {

    /** Controller for testing failover and undeploy **/
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    /**
     * Workaround for Arquillian so that you can use "@ArquillianResource(C.class) @OperateOnDeployment(D)" because the
     * containers need to be started beforehand.
     */
    @Test
    @InSequence(1)
    public void testStartContainersAndDeployments() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    /**
     * Test simple graceful shutdown failover:
     *
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Shutdown first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Bring up the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException 
     */
    @Test
    @InSequence(2)
    public void testGracefulSimpleFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, ExecutionException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "simple";
        String url2 = baseURL2.toString() + "simple";

        try {
            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            HttpResponse response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Gracefully shutdown the 1st container.
            controller.stop(CONTAINER_1);

            this.establishView(client, baseURL2, NODE_2);
            
            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.0'. Otherwise you will have to spoof cookies. @Rado

            response = client.execute(new HttpGet(url2));
            try {
                System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            try {
                System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            controller.start(CONTAINER_1);

            this.establishView(client, baseURL2, NODE_1, NODE_2);
            
            // Lets wait for the cluster to update membership and tranfer state.

            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 5, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

        // Is would be done automatically, keep for 2nd test is added
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);

        // Assert.fail("Show me the logs please!");
    }

    @Test
    @InSequence(3)
    public void testStartContainersAndDeploymentsForUndeployFailover() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    /**
     * Test simple undeploy failover:
     *
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Undeploy application from the first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Redeploy application to the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException 
     */
    @Test
    @InSequence(4)
    public void testGracefulUndeployFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "simple";
        String url2 = baseURL2.toString() + "simple";

        try {
            this.establishView(client, baseURL1, NODE_1, NODE_2);
            
            HttpResponse response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Gracefully undeploy from the 1st container.
            deployer.undeploy(DEPLOYMENT_1);

            this.establishView(client, baseURL2, NODE_2);
            
            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.1'. Otherwise you will have to spoof cookies. @Rado
            response = client.execute(new HttpGet(url2));
            try {
                System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            try {
                System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Redeploy
            deployer.deploy(DEPLOYMENT_1);

            this.establishView(client, baseURL2, NODE_1, NODE_2);
            
            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 5, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

        // Is would be done automatically, keep for when 3nd test is added
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);

        // Assert.fail("Show me the logs please!");
    }

    private void establishView(HttpClient client, URL baseURL, String... members) throws URISyntaxException, IOException {
        ClusterHttpClientUtil.establishView(client, baseURL, "web", members);
    }
}
