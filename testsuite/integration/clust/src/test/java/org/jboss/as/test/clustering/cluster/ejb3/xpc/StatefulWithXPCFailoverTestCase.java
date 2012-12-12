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

package org.jboss.as.test.clustering.cluster.ejb3.xpc;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.GRACE_TIME;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.cluster.ejb3.xpc.bean.StatefulBean;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;



/**
 * @author Paul Ferraro
 * @author Scott Marlow
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulWithXPCFailoverTestCase {

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "<property name=\"hibernate.cache.use_second_level_cache\" value=\"true\" />" +
            "<property name=\"hibernate.generate_statistics\" value=\"true\" />" +
            "<property name=\"hibernate.show_sql\" value=\"true\"/>" +
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
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "stateful.war");
        war.addPackage(StatefulBean.class.getPackage());
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulBean.class.getPackage(), "web.xml");
        war.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    @InSequence(1)
    public void testArquillianWorkaroundSecond() {
        // Container is unmanaged, need to start manually.
        start(DEPLOYMENT_1, CONTAINER_1);

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        start(DEPLOYMENT_2, CONTAINER_2);
    }

    @Test
    @InSequence(2)
    public void testBasicXPC(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException {

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        stop(DEPLOYMENT_2, CONTAINER_2);

        DefaultHttpClient client = HttpClientUtils.relaxedCookieHttpClient();

        String xpc1_create_url = baseURL1 + "count?command=createEmployee";
        String xpc1_get_url = baseURL1 + "count?command=getEmployee";
        String xpc2_get_url = baseURL2 + "count?command=getEmployee";
        String xpc1_getempsecond_url = baseURL1 + "count?command=getSecondBeanEmployee";
        String xpc2_getempsecond_url = baseURL2 + "count?command=getSecondBeanEmployee";
        String xpc2_getdestroy_url = baseURL2 + "count?command=destroy";

        try {
            // extended persistence context is available on node1

            System.out.println(new Date() + "create employee entity ");
            String employeeName = executeUrlWithAnswer(client, xpc1_create_url, "create entity that lives in the extended persistence context that this test will verify is always available");
            assertEquals(employeeName, "Tom Brady");

            System.out.println(new Date() + "1. about to read entity on node1");
            // ensure that we can get it from node 1
            employeeName = executeUrlWithAnswer(client, xpc1_get_url, "1. xpc on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc1_getempsecond_url, "1. xpc on node1, node1 should be able to read entity from second bean on node1");
            assertEquals(employeeName, "Tom Brady");

            start(DEPLOYMENT_2, CONTAINER_2);

            System.out.println(new Date() + "2. started node2 + deployed, about to read entity on node1");

            employeeName = executeUrlWithAnswer(client, xpc2_get_url, "2. started node2, xpc on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc2_getempsecond_url, "2. started node2, xpc on node1, node1 should be able to read entity from second bean on node1");
            assertEquals(employeeName, "Tom Brady");

            // failover to deployment2
            stop(DEPLOYMENT_1, CONTAINER_1); // failover #1 to node 2

            System.out.println(new Date() + "3. stopped node1 to force failover, about to read entity on node2");

            employeeName = executeUrlWithAnswer(client, xpc2_get_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc2_getempsecond_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc that is on node2 (second bean)");
            assertEquals(employeeName, "Tom Brady");

            String destroyed = executeUrlWithAnswer(client, xpc2_getdestroy_url, "4. destroy the bean on node2");
            assertEquals(destroyed, "destroy");
            System.out.println(new Date() + "4. test is done");

        } finally {
            client.getConnectionManager().shutdown();

            stop(DEPLOYMENT_1, CONTAINER_1);
            stop(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    /**
     * Use the second level cache statistics to ensure that deleting an entity on a cluster node, will
     * remove the entity from the second level cache on other nodes.
     *
     * Note that this test writes to the separate databases on both cluster nodes (so that both nodes can
     * read the entity from the database).  The important thing is that the second level cache entries are
     * invalidated when the entity is deleted from either database.
     *
     * @param baseURL1
     * @param baseURL2
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    @InSequence(3)
    public void testSecondLevelCache(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException {
        start(DEPLOYMENT_1, CONTAINER_1);
        start(DEPLOYMENT_2, CONTAINER_2);

        DefaultHttpClient client = new DefaultHttpClient();

        String xpc1_create_url = baseURL1 + "count?command=createEmployee";
        String xpc2_create_url = baseURL2 + "count?command=createEmployee";
        String xpc1_flush_url = baseURL1 + "count?command=flush";
        String xpc2_flush_url = baseURL2 + "count?command=flush";
        String xpc1_clear_url = baseURL1 + "count?command=clear";
        String xpc2_clear_url = baseURL2 + "count?command=clear";
        String xpc1_echo_url = baseURL1 + "count?command=echo&message=";
        String xpc2_echo_url = baseURL2 + "count?command=echo&message=";
        String xpc1_get_url = baseURL1 + "count?command=getEmployee";
        String xpc2_get_url = baseURL2 + "count?command=getEmployee";
        String xpc1_getdestroy_url = baseURL2 + "count?command=destroy";
        String xpc1_delete_url = baseURL1 + "count?command=deleteEmployee";
        String xpc2_delete_url = baseURL2 + "count?command=deleteEmployee";
        String xpc1_secondLevelCacheEntries_url = baseURL1 + "count?command=getEmployeesInSecondLevelCache";
        String xpc2_secondLevelCacheEntries_url = baseURL2 + "count?command=getEmployeesInSecondLevelCache";

        try {
            assertExecuteUrl(client, xpc1_echo_url + "StartingTestSecondLevelCache");  // echo message to server.log
            assertExecuteUrl(client,xpc2_echo_url + "StartingTestSecondLevelCache"); // echo message to server.log

            String employeeName = executeUrlWithAnswer(client, xpc1_create_url, "create entity in node1 in memory db");                           //
            assertEquals(employeeName, "Tom Brady");
            System.out.println(new Date() + "about to read entity on node1 (from xpc queue)");

            employeeName = executeUrlWithAnswer(client, xpc1_get_url, "on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");

            String employeesInCache = executeUrlWithAnswer(client,xpc1_secondLevelCacheEntries_url, "get number of elements in node1 second level cache (should be zero)");
            assertEquals(employeesInCache, "0");    // we read the entity from the extended persistence context (hasn't been flushed yet)

            assertExecuteUrl(client, xpc1_flush_url); // flush changes to db

            assertExecuteUrl(client, xpc1_clear_url); // clear xpc state so we have to reload

            employeeName = executeUrlWithAnswer(client, xpc2_create_url, "create entity in node2 in memory db (each node has its own database)");
            assertEquals(employeeName, "Tom Brady");
            assertExecuteUrl(client, xpc2_flush_url); // flush changes to db on second node
            assertExecuteUrl(client, xpc2_clear_url); // clear xpc state so we have to reload
            employeeName = executeUrlWithAnswer(client, xpc2_get_url, "node2 should be able to read entity from 2lc");
            assertEquals(employeeName, "Tom Brady");

            // we should of read one Employee entity on node2, ensure the second level cache contains one entry
            employeesInCache = executeUrlWithAnswer(client,xpc2_secondLevelCacheEntries_url, "get number of elements in node2 second level cache (should be zero)");
            assertEquals(employeesInCache, "1");

            assertExecuteUrl(client,xpc1_echo_url + "testSecondLevelCacheclearedXPC");
            assertExecuteUrl(client,xpc2_echo_url + "testSecondLevelCacheclearedXPC");

            assertExecuteUrl(client,xpc2_delete_url);   // deleting the entity on one node should remove it from both nodes second level cache
            assertExecuteUrl(client,xpc1_echo_url + "testSecondLevelCachedeletedEnityOnNode2");
            assertExecuteUrl(client,xpc1_echo_url + "2lcOnNode1ShouldHaveZeroElemementsLoaded");

            employeesInCache = executeUrlWithAnswer(client,xpc1_secondLevelCacheEntries_url, "get number of elements in node1 second level cache (should be zero)");
            assertEquals(employeesInCache, "0");

            employeesInCache = executeUrlWithAnswer(client,xpc2_secondLevelCacheEntries_url, "get number of elements in node2 second level cache (should be zero)");
            assertEquals(employeesInCache, "0");

            assertExecuteUrl(client, xpc1_delete_url);

            String destroyed = executeUrlWithAnswer(client, xpc1_getdestroy_url, "destroy the bean on node1");
            assertEquals(destroyed, "destroy");

        } finally {
            client.getConnectionManager().shutdown();

            stop(DEPLOYMENT_1, CONTAINER_1);
            stop(DEPLOYMENT_2, CONTAINER_2);
        }
    }

    private String executeUrlWithAnswer(DefaultHttpClient client, String url, String message) throws IOException, InterruptedException {
        int maxWait = GRACE_TIME;
        while (maxWait > 0) {
            HttpResponse response = client.execute(new HttpGet(url));
            try {
                if (response.getStatusLine().getStatusCode() < 400 || response.getStatusLine().getStatusCode() > 500) {
                    assertEquals(200, response.getStatusLine().getStatusCode());
                    Header header = response.getFirstHeader("answer");
                    if (header != null) {
                        return header.getValue();
                    }
                    throw new AssertionError("assertExecuteUrlWithResult didn't get expected answer from executed url=" + url + ", " + message);
                }
            } finally {
                response.getEntity().getContent().close();
            }
            maxWait -= 100;
            Thread.sleep(100);
        }
        throw new AssertionError("assertExecuteUrlWithResult Timed out trying to execute url=" + url +", " + message);

    }

    private void assertExecuteUrl(DefaultHttpClient client, String url) throws IOException, InterruptedException {
        int maxWait = GRACE_TIME;
        while (maxWait > 0) {
            HttpResponse response = client.execute(new HttpGet(url));
            try {
                if (response.getStatusLine().getStatusCode() < 400 || response.getStatusLine().getStatusCode() > 500) {
                    assertEquals(200, response.getStatusLine().getStatusCode());
                    return;
                }
            } finally {
                response.getEntity().getContent().close();
            }
            maxWait -= 100;
            Thread.sleep(100);
        }
        throw new AssertionError("assertExecuteUrl Timed out trying to execute url=" + url);
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
