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

package org.jboss.as.test.clustering.unmanaged.ejb3.stateful;

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
import org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean.StatefulBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulFailoverTestCase {
    /** Constants **/
    public static final long GRACE_TIME_TO_MEMBERSHIP_CHANGE = 5000;
    public static final long GRACE_TIME_TO_REPLICATE = 2000;
    public static final String CONTAINER1 = "clustering-udp-0-unmanaged";
    public static final String CONTAINER2 = "clustering-udp-1-unmanaged";
    public static final String DEPLOYMENT1 = "deployment-0-unmanaged";
    public static final String DEPLOYMENT2 = "deployment-1-unmanaged";

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
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Deployment(name = DEPLOYMENT2, managed = false, testable = false)
    @TargetsContainer(CONTAINER2)
    public static Archive<?> deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "force-hashcode-change.txt");
        System.out.println(war.toString(true));
        return war;
    }
    
    @Test
    @InSequence(1)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void test(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER1);
        deployer.deploy(DEPLOYMENT1);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, second hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String url1 = "http://127.0.0.1:8080/stateful/count"; /* baseURL.toString() + "simple"; */
        String url2 = "http://127.0.0.1:8180/stateful/count";

        try {
            HttpResponse response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            controller.start(CONTAINER2);
            deployer.deploy(DEPLOYMENT2);

            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(5, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            controller.stop(CONTAINER2);
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(7, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(8, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            controller.start(CONTAINER2);
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(9, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(10, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            Thread.sleep(GRACE_TIME_TO_REPLICATE);
            
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(11, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();
            
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(12, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            controller.stop(CONTAINER1);
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(13, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(14, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();
            
            controller.start(CONTAINER1);
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(15, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(16, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();

            Thread.sleep(GRACE_TIME_TO_REPLICATE);
            
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(17, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();
            
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(18, Integer.parseInt(response.getFirstHeader("count").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();

            deployer.undeploy(DEPLOYMENT1);
            controller.stop(CONTAINER1);
            deployer.undeploy(DEPLOYMENT2);
            controller.stop(CONTAINER2);
        }
    }
}
