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

package org.jboss.as.test.clustering.unmanaged.ejb3.xpc;

import java.io.IOException;
import java.util.Date;
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
import org.jboss.as.test.clustering.unmanaged.ejb3.xpc.bean.StatefulBean;
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
public class StatefulWithXPCFailoverTestCase {
    /** Constants **/
    public static final int GRACE_TIME = 20000;
    public static final String CONTAINER_1 = "clustering-udp-1-unmanaged";
    public static final String CONTAINER_2 = "clustering-udp-2-unmanaged";
    public static final String DEPLOYMENT_1 = "deployment-1-unmanaged";
    public static final String DEPLOYMENT_2 = "deployment-2-unmanaged";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @ArquillianResource
    ContainerController controller;
    @ArquillianResource
    Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        System.out.println(war.toString(true));
        return war;
    }


    @Test
    @InSequence(1)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void testBasicXPC(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        start(DEPLOYMENT_1, CONTAINER_1);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, node2 (port 8180) hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String xpc1_create_url = "http://127.0.0.1:8080/stateful/count?command=createEmployee";
        String xpc1_get_url = "http://127.0.0.1:8080/stateful/count?command=getEmployee";
        String xpc2_get_url = "http://127.0.0.1:8180/stateful/count?command=getEmployee";
        String xpc1_getempsecond_url = "http://127.0.0.1:8080/stateful/count?command=getSecondBeanEmployee";
        String xpc2_getempsecond_url = "http://127.0.0.1:8180/stateful/count?command=getSecondBeanEmployee";
        String xpc2_getdestroy_url = "http://127.0.0.1:8180/stateful/count?command=destroy";

        try {
            // extended persistence context is available on node1

            System.out.println(new Date() + "create employee entity ");
            // create entity that lives in the extended persistence context that this test will verify is always available.
            assertCreateEmployee(client, xpc1_create_url);

            System.out.println(new Date() + "1. about to read entity on node1");
            // ensure that we can get it from node 1
            assertGetEmployee(client, xpc1_get_url, "1. xpc on node1, node1 should be able to read entity on node1");
            assertGetSecondEmployee(client, xpc1_getempsecond_url, "1. xpc on node1, node1 should be able to read entity from second bean on node1");

            start(DEPLOYMENT_2, CONTAINER_2);

            System.out.println(new Date() + "2. started node2 + deployed, about to read entity on node1");

            // ensure that we can still get it from node 1
            assertGetEmployee(client, xpc1_get_url, "2. started node2, xpc on node1, node1 should be able to read entity on node1");
            assertGetSecondEmployee(client, xpc1_getempsecond_url, "2. started node2, xpc on node1, node1 should be able to read entity from second bean on node1");

            // failover to deployment2
            stop(DEPLOYMENT_1, CONTAINER_1); // failover #1 to node 2

            System.out.println(new Date() + "3. stopped node1 to force failover, about to read entity on node2");

            assertGetEmployee(client, xpc2_get_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc");
            assertGetEmployee(client, xpc2_getempsecond_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc that is on node2 (second bean)");

            // restart deployment1
            start(DEPLOYMENT_1, CONTAINER_1);

            System.out.println(new Date() + "4. started node1, about to read entity on node2");

            // to cause the java.io.OptionalDataException.&lt;init&gt;(OptionalDataException.java:51), comment out the following two lines
            assertGetEmployee(client, xpc2_get_url, "4. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc");
            assertGetEmployee(client, xpc2_getempsecond_url, "4. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc that is on node2 (second bean)");

            // failover to deployment1
            stop(DEPLOYMENT_2, CONTAINER_2); // failover #1 to node 1

            System.out.println(new Date() + "5. stopped node2, about to read entity on node1");

            assertGetEmployee(client, xpc1_get_url, "5. stopped deployment on node2, xpc still on node1, node1 should be able to read entity from xpc");
            assertGetSecondEmployee(client, xpc1_getempsecond_url, "5. stopped deployment on node2, xpc still on node1, node1 should be able to read entity from xpc of second bean");

            start(DEPLOYMENT_2, CONTAINER_2);

            System.out.println(new Date() + "6. started node2, about to read entity on node1");

            assertGetEmployee(client, xpc1_get_url, "6. xpc still on node1, node1 should be able to read entity from xpc");
            assertGetSecondEmployee(client, xpc1_getempsecond_url, "6. xpc still on node1, node1 should be able to read entity from xpc of second bean");

            // failover to deployment2
            stop(DEPLOYMENT_1, CONTAINER_1);  // failover #2 to node 2

            System.out.println(new Date() + "7. stopped node1, about to read entity on node2");

            assertGetEmployee(client, xpc2_get_url, "7. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc");
            assertGetEmployee(client, xpc2_getempsecond_url, "7. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc that is on node2 (second bean)");

            assertDestroy(client, xpc2_getdestroy_url,"destroy the bean on node2");


        } finally {
            client.getConnectionManager().shutdown();

            stop(DEPLOYMENT_1, CONTAINER_1);
            stop(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    private String createEmployee(HttpClient client, String url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 500)
               return null;

            assertEquals(200, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("employee").getValue();
        } finally {
            response.getEntity().getContent().close();
        }
    }

    private String getEmployee(HttpClient client, String url, String message) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 500)
               return null;

            assertEquals(message + "(didn't get http success code) ", 200, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("employee").getValue();
        } finally {
            response.getEntity().getContent().close();
        }
    }

    private String getSecondEmployee(HttpClient client, String url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 500)
               return null;

            assertEquals(200, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("employee").getValue();
        } finally {
            response.getEntity().getContent().close();
        }
    }

    private String getDestroy(HttpClient client, String url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 500)
               return null;

            assertEquals(200, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("employee").getValue();
        } finally {
            response.getEntity().getContent().close();
        }
    }


    private void assertCreateEmployee(HttpClient client, String url) throws IOException, InterruptedException {
           int maxWait = GRACE_TIME;
           String name = null;
           while (maxWait > 0) {
               Thread.sleep(1000);

               name = createEmployee(client, url);
               if (name != null) {
                   break;
               }
               maxWait -= 1000;
          }

          if (name == null)
              throw new AssertionError("assertCreateEmployee Timed out waiting for a result");

          assertEquals(name, "Tom Brady");
      }

    private void assertGetEmployee(HttpClient client, String url, String message) throws IOException, InterruptedException {
           int maxWait = GRACE_TIME;
           String name = null;
           while (maxWait > 0) {
               Thread.sleep(100);

               name = getEmployee(client, url, message);
               if (name != null) {
                   break;
               }
               maxWait -= 100;
          }

          if (name == null)
              throw new AssertionError("assertGetEmployee timed out waiting for a result");

          assertEquals(message, name, "Tom Brady");
      }

    private void assertGetSecondEmployee(HttpClient client, String url, String message) throws IOException, InterruptedException {
           int maxWait = GRACE_TIME;
           String name = null;
           while (maxWait > 0) {
               Thread.sleep(100);

               name = getSecondEmployee(client, url);
               if (name != null) {
                   break;
               }
               maxWait -= 100;
          }

          if (name == null)
              throw new AssertionError("assertGetSecondEmployee timed out waiting for a result");

          assertEquals(message, name, "Tom Brady");
      }

    private void assertDestroy(HttpClient client, String url, String message) throws IOException, InterruptedException {
           int maxWait = GRACE_TIME;
           String name = null;
           while (maxWait > 0) {
               Thread.sleep(100);

               name = getDestroy(client, url);
               if (name != null) {
                   break;
               }
               maxWait -= 100;
          }

          if (name == null)
              throw new AssertionError("assertDestroy timed out waiting for a result " + message);

          assertEquals(message, name, "destroy");
      }

    private void stop(String deployment, String container) {
        try {
            System.out.println(new Date() + "stopping deployment="+deployment+", container="+container);
            deployer.undeploy(deployment);
            controller.stop(container);
            System.out.println(new Date() + "stopped deployment="+deployment+", container="+container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    private void start(String deployment, String container) {
        try {
            System.out.println(new Date() + "starting deployment="+deployment+", container="+ container);
            controller.start(container);
            deployer.deploy(deployment);
            System.out.println(new Date() + "started deployment="+deployment+", container=" + container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

}
