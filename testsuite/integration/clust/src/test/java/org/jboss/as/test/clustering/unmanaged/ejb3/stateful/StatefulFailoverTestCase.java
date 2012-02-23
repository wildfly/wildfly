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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
import org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean.CounterDecorator;
import org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean.StatefulBean;
import org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean.StatefulCDIInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul Ferraro
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulFailoverTestCase {
    /** Constants **/
    public static final int GRACE_TIME = 20000;
    public static final String CONTAINER_1 = "clustering-udp-1-unmanaged";
    public static final String CONTAINER_2 = "clustering-udp-2-unmanaged";
    public static final String[] CONTAINERS = new String[] { CONTAINER_1, CONTAINER_2 };
    public static final String DEPLOYMENT_1 = "deployment-1-unmanaged";
    public static final String DEPLOYMENT_2 = "deployment-2-unmanaged";
    public static final String[] DEPLOYMENTS = new String[] { DEPLOYMENT_1, DEPLOYMENT_2 };

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        WebArchive war = createDeployment();
        return war;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        WebArchive war = createDeployment();
        return war;
    }

    private static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsWebInfResource(new StringAsset("<beans>" +
                "<interceptors><class>" + StatefulCDIInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + CounterDecorator.class.getName() + "</class></decorators>" +
                "</beans>"), "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    @InSequence(1)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void testRestart(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, second hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String url1 = "http://127.0.0.1:8080/stateful/count";
        String url2 = "http://127.0.0.1:8180/stateful/count";

        try {
            assertQueryCount(20010101, client, url1);
            assertQueryCount(20020202, client, url1);

            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);

            assertQueryCount(20030303, client, url1);
            assertQueryCount(20040404, client, url1);

            assertQueryCount(20050505, client, url2);
            assertQueryCount(20060606, client, url2);

            controller.stop(CONTAINER_2);

            assertQueryCount(20070707, client, url1);
            assertQueryCount(20080808, client, url1);

            controller.start(CONTAINER_2);

            assertQueryCount(20090909, client, url1);
            assertQueryCount(20101010, client, url1);

            assertQueryCount(20111111, client, url2);
            assertQueryCount(20121212, client, url2);

            controller.stop(CONTAINER_1);
            assertQueryCount(20131313, client, url2);
            assertQueryCount(20141414, client, url2);

            controller.start(CONTAINER_1);

            assertQueryCount(20151515, client, url1);
            assertQueryCount(20161616, client, url1);

            assertQueryCount(20171717, client, url1);
            assertQueryCount(20181818, client, url1);
        } finally {
            client.getConnectionManager().shutdown();

            this.cleanup(DEPLOYMENT_1, CONTAINER_1);
            this.cleanup(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    @Test
    @InSequence(2)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void testRedeploy(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, second hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String url1 = "http://127.0.0.1:8080/stateful/count";
        String url2 = "http://127.0.0.1:8180/stateful/count";

        try {
            assertQueryCount(20010101, client, url1);
            assertQueryCount(20020202, client, url1);

            controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);

            assertQueryCount(20030303, client, url1);
            assertQueryCount(20040404, client, url1);

            assertQueryCount(20050505, client, url2);
            assertQueryCount(20060606, client, url2);

            deployer.undeploy(DEPLOYMENT_2);

            assertQueryCount(20070707, client, url1);
            assertQueryCount(20080808, client, url1);

            deployer.deploy(DEPLOYMENT_2);

            assertQueryCount(20090909, client, url1);
            assertQueryCount(20101010, client, url1);

            assertQueryCount(20111111, client, url2);
            assertQueryCount(20121212, client, url2);

            deployer.undeploy(DEPLOYMENT_1);

            assertQueryCount(20131313, client, url2);
            assertQueryCount(20141414, client, url2);

            deployer.deploy(DEPLOYMENT_1);

            assertQueryCount(20151515, client, url1);
            assertQueryCount(20161616, client, url1);

            assertQueryCount(20171717, client, url2);
            assertQueryCount(20181818, client, url2);
        } finally {
            client.getConnectionManager().shutdown();

            this.cleanup(DEPLOYMENT_1, CONTAINER_1);
            this.cleanup(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    private int queryCount(HttpClient client, String url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 500)
               return -1;

            assertEquals(200, response.getStatusLine().getStatusCode());
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        } finally {
            response.getEntity().getContent().close();
        }
    }

    private void cleanup(String deployment, String container) {
        try {
            this.deployer.undeploy(deployment);
            this.controller.stop(container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    private void assertQueryCount(int i, HttpClient client, String url) throws IOException, InterruptedException {
           int maxWait = GRACE_TIME;
           int count = 0;
           while (maxWait > 0) {
               Thread.sleep(100);

               count = queryCount(client, url);
               if (count >= 0) break;
               maxWait -= 100;
          }

          if (count == -1)
              throw new AssertionError("Timed out waiting for a result");

          assertEquals(i, count);
      }
}
