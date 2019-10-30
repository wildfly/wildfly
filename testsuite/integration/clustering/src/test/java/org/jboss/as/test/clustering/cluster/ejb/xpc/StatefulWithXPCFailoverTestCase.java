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

package org.jboss.as.test.clustering.cluster.ejb.xpc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.xpc.bean.StatefulBean;
import org.jboss.as.test.clustering.cluster.ejb.xpc.servlet.StatefulServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class StatefulWithXPCFailoverTestCase extends AbstractClusteringTestCase {

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

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, StatefulBean.MODULE + ".war");
        war.addPackage(StatefulBean.class.getPackage());
        war.addPackage(StatefulServlet.class.getPackage());
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulServlet.class.getPackage(), "web.xml");
        war.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return war;
    }

    /**
     * Use the second level cache statistics to ensure that deleting an entity on a cluster node, will
     * remove the entity from the second level cache on other nodes.
     * <p/>
     * Note that this test writes to the separate databases on both cluster nodes (so that both nodes can
     * read the entity from the database).  The important thing is that the second level cache entries are
     * invalidated when the entity is deleted from either database.
     *
     * @param baseURL1
     * @param baseURL2
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testSecondLevelCache(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        URI xpc1_create_url = StatefulServlet.createEmployeeURI(baseURL1);
        URI xpc2_create_url = StatefulServlet.createEmployeeURI(baseURL2);
        URI xpc1_flush_url = StatefulServlet.flushURI(baseURL1);
        URI xpc2_flush_url = StatefulServlet.flushURI(baseURL2);
        URI xpc1_clear_url = StatefulServlet.clearURI(baseURL1);
        URI xpc2_clear_url = StatefulServlet.clearURI(baseURL2);
        URI xpc1_get_url = StatefulServlet.getEmployeeURI(baseURL1);
        URI xpc2_get_url = StatefulServlet.getEmployeeURI(baseURL2);
        URI xpc1_getdestroy_url = StatefulServlet.destroyURI(baseURL2);
        URI xpc1_delete_url = StatefulServlet.deleteEmployeeURI(baseURL1);
        URI xpc2_delete_url = StatefulServlet.deleteEmployeeURI(baseURL2);
        URI xpc1_secondLevelCacheEntries_url = StatefulServlet.getEmployeesIn2LCURI(baseURL1);
        URI xpc2_secondLevelCacheEntries_url = StatefulServlet.getEmployeesIn2LCURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL1, "StartingTestSecondLevelCache"));  // echo message to server.log
            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL2, "StartingTestSecondLevelCache")); // echo message to server.log

            String employeeName = executeUrlWithAnswer(client, xpc1_create_url, "create entity in node1 in memory db");
            assertEquals(employeeName, "Tom Brady");
            log.trace(new Date() + "about to read entity on node1 (from xpc queue)");

            employeeName = executeUrlWithAnswer(client, xpc1_get_url, "on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");

            String employeesInCache = executeUrlWithAnswer(client, xpc1_secondLevelCacheEntries_url, "get number of elements in node1 second level cache (should be zero)");
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
            employeesInCache = executeUrlWithAnswer(client, xpc2_secondLevelCacheEntries_url, "get number of elements in node2 second level cache (should be zero)");
            assertEquals(employeesInCache, "1");

            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL1, "testSecondLevelCacheclearedXPC"));
            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL2, "testSecondLevelCacheclearedXPC"));

            assertExecuteUrl(client, xpc2_delete_url);   // deleting the entity on one node should remove it from both nodes second level cache
            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL1, "testSecondLevelCachedeletedEnityOnNode2"));
            assertExecuteUrl(client, StatefulServlet.echoURI(baseURL1, "2lcOnNode1ShouldHaveZeroElemementsLoaded"));

            employeesInCache = executeUrlWithAnswer(client, xpc1_secondLevelCacheEntries_url, "get number of elements in node1 second level cache (should be zero)");
            assertEquals(employeesInCache, "0");

            employeesInCache = executeUrlWithAnswer(client, xpc2_secondLevelCacheEntries_url, "get number of elements in node2 second level cache (should be zero)");
            assertEquals(employeesInCache, "0");

            assertExecuteUrl(client, xpc1_delete_url);

            String destroyed = executeUrlWithAnswer(client, xpc1_getdestroy_url, "destroy the bean on node1");
            assertEquals(destroyed, "DESTROY");

        }
    }

    @Test
    public void testBasicXPC(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        URI xpc1_create_url = StatefulServlet.createEmployeeURI(baseURL1);
        URI xpc1_get_url = StatefulServlet.getEmployeeURI(baseURL1);
        URI xpc2_get_url = StatefulServlet.getEmployeeURI(baseURL2);
        URI xpc1_getempsecond_url = StatefulServlet.get2ndBeanEmployeeURI(baseURL1);
        URI xpc2_getempsecond_url = StatefulServlet.get2ndBeanEmployeeURI(baseURL2);
        URI xpc2_getdestroy_url = StatefulServlet.destroyURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            stop(NODE_2);

            // extended persistence context is available on node1

            log.trace(new Date() + "create employee entity ");
            String employeeName = executeUrlWithAnswer(client, xpc1_create_url, "create entity that lives in the extended persistence context that this test will verify is always available");
            assertEquals(employeeName, "Tom Brady");

            log.trace(new Date() + "1. about to read entity on node1");
            // ensure that we can get it from node 1
            employeeName = executeUrlWithAnswer(client, xpc1_get_url, "1. xpc on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc1_getempsecond_url, "1. xpc on node1, node1 should be able to read entity from second bean on node1");
            assertEquals(employeeName, "Tom Brady");

            start(NODE_2);

            log.trace(new Date() + "2. started node2 + deployed, about to read entity on node1");

            employeeName = executeUrlWithAnswer(client, xpc2_get_url, "2. started node2, xpc on node1, node1 should be able to read entity on node1");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc2_getempsecond_url, "2. started node2, xpc on node1, node1 should be able to read entity from second bean on node1");
            assertEquals(employeeName, "Tom Brady");

            // failover to deployment2
            stop(NODE_1); // failover #1 to node 2

            log.trace(new Date() + "3. stopped node1 to force failover, about to read entity on node2");

            employeeName = executeUrlWithAnswer(client, xpc2_get_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc");
            assertEquals(employeeName, "Tom Brady");
            employeeName = executeUrlWithAnswer(client, xpc2_getempsecond_url, "3. stopped deployment on node1, xpc should failover to node2, node2 should be able to read entity from xpc that is on node2 (second bean)");
            assertEquals(employeeName, "Tom Brady");

            String destroyed = executeUrlWithAnswer(client, xpc2_getdestroy_url, "4. destroy the bean on node2");
            assertEquals(destroyed, "DESTROY");
            log.trace(new Date() + "4. test is done");

        }
    }

    private static String executeUrlWithAnswer(HttpClient client, URI url, String message) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Header header = response.getFirstHeader("answer");
            Assert.assertNotNull(message, header);
            return header.getValue();
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private static void assertExecuteUrl(HttpClient client, URI url) throws IOException {
        HttpResponse response = client.execute(new HttpGet(url));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }
}
