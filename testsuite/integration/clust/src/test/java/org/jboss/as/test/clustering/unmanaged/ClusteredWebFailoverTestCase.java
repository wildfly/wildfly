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
package org.jboss.as.test.clustering.unmanaged;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebTestCase;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

/**
 * Test that failover and undeploy works.
 * 
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredWebFailoverTestCase {

    /** Logger **/
    private static final Logger log = Logger.getLogger(ClusteredWebFailoverTestCase.class);
    /** Constants **/
    public static final long GRACE_TIME_TO_REPLICATE = 3000; // 3 seconds should be more then enough
    public static final long GRACE_TIME_TO_MEMBERSHIP_CHANGE = 10000;
    public static final String CONTAINER1 = "clustering-udp-0-unmanaged";
    public static final String CONTAINER2 = "clustering-udp-1-unmanaged";
    public static final String DEPLOYMENT1 = "deployment-0-unmanaged";
    public static final String DEPLOYMENT2 = "deployment-1-unmanaged";
    /** Controller for testing failover and undeploy **/
    @ArquillianResource
    ContainerController controller;
    @ArquillianResource
    Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    @Deployment(name = DEPLOYMENT1, managed = false, testable = false)
    @TargetsContainer(CONTAINER1)
    public static Archive<?> deployment0() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebTestCase.class.getPackage(), "web.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Deployment(name = DEPLOYMENT2, managed = false, testable = false)
    @TargetsContainer(CONTAINER2)
    public static Archive<?> deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        war.setWebXML(ClusteredWebTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "force-hashcode-change.txt");
        System.out.println(war.toString(true));
        return war;
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
     */
    @Test
    @Ignore("AS7-2912")
    @InSequence(1)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void testGracefulSimpleFailover(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER1);
        deployer.deploy(DEPLOYMENT1);

        controller.start(CONTAINER2);
        deployer.deploy(DEPLOYMENT2);

        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, second hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String url1 = "http://127.0.0.1:8080/distributable/simple"; /* baseURL.toString() + "simple"; */
        String url2 = "http://127.0.0.1:8180/distributable/simple";

        try {
            HttpResponse response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Gracefully shutdown the 1st container.
            controller.stop(CONTAINER1);

            // Lets wait for the session to replicate, we dont care about membership now.
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.0'. Otherwise you will have to spoof cookies. @Rado
            response = client.execute(new HttpGet(url2));
            System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            controller.start(CONTAINER1);

            // Lets wait for the cluster to update membership and tranfer state.
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 5, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }

        // Is would be done automatically, keep for 2nd test is added
        controller.stop(CONTAINER1);
        controller.stop(CONTAINER2);

        // Assert.fail("Show me the logs please!");
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
     */
    @Test
    @Ignore("AS7-2912")
    @InSequence(2)
    public void testGracefulUndeployFailover() throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER1);
        deployer.deploy(DEPLOYMENT1);

        controller.start(CONTAINER2);
        deployer.deploy(DEPLOYMENT2);

        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        DefaultHttpClient client = new DefaultHttpClient();

        // TODO ARQ-674
        String url1 = "http://127.0.0.1:8080/distributable/simple";
        String url2 = "http://127.0.0.1:8180/distributable/simple";

        try {
            HttpResponse response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Gracefully undeploy from the 1st container.
            deployer.undeploy(DEPLOYMENT1);

            // Lets wait for the session to replicate, we dont care about membership now.
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.0'. Otherwise you will have to spoof cookies. @Rado
            response = client.execute(new HttpGet(url2));
            System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            System.out.println("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Redeploy
            deployer.deploy(DEPLOYMENT1);

            // Lets wait for the cluster to update membership and tranfer state.
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 5, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            System.out.println("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }

        // Is would be done automatically, keep for when 3nd test is added
        controller.stop(CONTAINER1);
        controller.stop(CONTAINER2);

        // Assert.fail("Show me the logs please!");
    }
}
