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
package org.jboss.as.test.integration.management.http;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.HttpMgmtProxy;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * Tests all management operation types which are available via HTTP POST requests.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(WildflyTestRunner.class)
public class HttpPostMgmtOpsTestCase {

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    private HttpMgmtProxy httpMgmt;

    @Inject
    protected ManagementClient managementClient;


    @Before
    public void before() throws Exception {
        URL mgmtURL = new URL("http", managementClient.getMgmtAddress(), MGMT_PORT, MGMT_CTX);
        httpMgmt = new HttpMgmtProxy(mgmtURL);
    }

    @Test
    public void testReadResource() throws Exception {
        testReadResource(false);
    }

    @Test
    public void testReadResourceRecursive() throws Exception {
        testReadResource(true);
    }

    private void testReadResource(boolean recursive) throws Exception {

        ModelNode op = httpMgmt.getOpNode("subsystem=logging", "read-resource");
        if (recursive) { op.get("recursive").set(true); }

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        assertTrue(result.has("root-logger"));
        ModelNode rootLogger = result.get("root-logger");


        assertTrue(rootLogger.has("ROOT"));
        ModelNode root = rootLogger.get("ROOT");
        if (recursive) {
            assertTrue(root.has("level"));
            ModelNode level = root.get("level");
            assertTrue(level.isDefined());

            assertTrue(root.has("handlers"));
            ModelNode handlers = root.get("handlers");

            assertFalse(handlers.asList().isEmpty());
        }

    }

    @Test
    public void testReadAttribute() throws Exception {

        ModelNode op = httpMgmt.getOpNode("subsystem=logging", "read-attribute");
        op.get("name").set("add-logging-api-dependencies");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");
        // check that a boolean is returned
        assertEquals(result.getType(), ModelType.BOOLEAN);

    }

    @Test
    public void testReadResourceDescription() throws Exception {

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=logging", "read-resource-description");
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");
        assertTrue(result.has("description"));
        assertTrue(result.has("attributes"));
    }

    @Test
    public void testReadOperationNames() throws Exception {

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=logging", "read-operation-names");
        assertTrue("success".equals(ret.get("outcome").asString()));

        List<ModelNode> names = ret.get("result").asList();

        System.out.println(names.toString());
        Set<String> strNames = new TreeSet<String>();
        for (ModelNode n : names) { strNames.add(n.asString()); }

        assertTrue(strNames.contains("read-attribute"));
        assertTrue(strNames.contains("read-children-names"));
        assertTrue(strNames.contains("read-children-resources"));
        assertTrue(strNames.contains("read-children-types"));
        assertTrue(strNames.contains("read-operation-description"));
        assertTrue(strNames.contains("read-operation-names"));
        assertTrue(strNames.contains("read-resource"));
        assertTrue(strNames.contains("read-resource-description"));
        assertTrue(strNames.contains("write-attribute"));

    }

    @Test
    public void testReadOperationDescription() throws Exception {

        ModelNode op = httpMgmt.getOpNode("subsystem=logging", "read-operation-description");
        op.get("name").set("add");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        assertTrue(result.has("operation-name"));
        assertTrue(result.has("description"));
        assertTrue(result.has("request-properties"));
    }

    @Test
    public void testReadChildrenTypes() throws Exception {

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=logging", "read-children-types");
        assertTrue("success".equals(ret.get("outcome").asString()));
        ModelNode result = ret.get("result");

        Set<String> strNames = new TreeSet<String>();
        for (ModelNode n : result.asList()) {
            strNames.add(n.asString());
        }


        assertTrue(strNames.contains("logging-profile"));
        assertTrue(strNames.contains("logger"));
    }

    @Test
    public void testReadChildrenNames() throws Exception {

        ModelNode op = HttpMgmtProxy.getOpNode("subsystem=logging", "read-children-names");
        op.get("child-type").set("root-logger");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        List<ModelNode> names = result.asList();
        Set<String> strNames = new TreeSet<>();
        for (ModelNode n : names) { strNames.add(n.asString()); }

        assertTrue(strNames.contains("ROOT"));

    }

    @Test
    public void testReadChildrenResources() throws Exception {

        ModelNode op = HttpMgmtProxy.getOpNode("subsystem=logging", "read-children-resources");
        op.get("child-type").set("logger");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        assertFalse(result.asList().isEmpty());
    }

    @Test
    @Ignore("needs to be moved somewhere else")
    public void testAddRemoveOperation() throws Exception {

        // add new connector

        ModelNode op = httpMgmt.getOpNode("socket-binding-group=standard-sockets/socket-binding=test", "add");
        op.get("port").set(8181);
        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        op = httpMgmt.getOpNode("subsystem=undertow/server=default-server/http-listener=test-listener", "add");
        op.get("socket-binding").set("test");
        op.get("enabled").set(true);

        ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        // check that the connector is live
        String cURL = "http://localhost:8181";

        String response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);


        // remove connector
        ModelNode operation = HttpMgmtProxy.getOpNode("subsystem=undertow/server=default-server/http-listener=test-listener", "remove");
        operation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ret = httpMgmt.sendPostCommand(operation);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ret = httpMgmt.sendPostCommand("socket-binding-group=standard-sockets/socket-binding=test", "remove");
        assertTrue("success".equals(ret.get("outcome").asString()));

        // check that the connector is no longer live
        Thread.sleep(5000);
        boolean failed = false;
        try {
            response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
        }
        assertTrue("Connector still live: " + response, failed);
    }
}
