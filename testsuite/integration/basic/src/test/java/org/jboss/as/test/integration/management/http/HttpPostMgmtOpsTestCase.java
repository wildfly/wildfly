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
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.HttpMgmtProxy;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests all management operation types which are available via HTTP POST requests.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpPostMgmtOpsTestCase {

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    @ArquillianResource
    URL url;
    private HttpMgmtProxy httpMgmt;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(HttpPostMgmtOpsTestCase.class);
        return ja;
    }

    @Before
    public void before() throws Exception {
        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
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

        ModelNode op = httpMgmt.getOpNode("subsystem=undertow", "read-resource");
        if (recursive) { op.get("recursive").set(true); }

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        assertTrue(result.has("server"));
        ModelNode server = result.get("server");


        assertTrue(server.has("default-server"));
        ModelNode vServer = server.get("default-server");
        if (recursive) {
            assertTrue(vServer.has("host"));
            ModelNode hosts = vServer.get("host");

            assertTrue(hosts.has("default-host"));
            ModelNode host = hosts.get("default-host");

            assertTrue(host.has("alias"));
        }

    }

    @Test
    public void testReadAttribute() throws Exception {

        ModelNode op = httpMgmt.getOpNode("subsystem=undertow", "read-attribute");
        op.get("name").set("default-servlet-container");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");
        // check that a boolean is returned
        assertEquals(result.getType(), ModelType.STRING);

    }

    @Test
    public void testReadResourceDescription() throws Exception {

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=undertow", "read-resource-description");
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");
        assertTrue(result.has("description"));
        assertTrue(result.has("attributes"));
    }

    @Test
    public void testReadOperationNames() throws Exception {

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=undertow", "read-operation-names");
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

        ModelNode op = httpMgmt.getOpNode("subsystem=undertow", "read-operation-description");
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

        ModelNode ret = httpMgmt.sendPostCommand("subsystem=undertow", "read-children-types");
        assertTrue("success".equals(ret.get("outcome").asString()));
        ModelNode result = ret.get("result");

        Set<String> strNames = new TreeSet<String>();
        for (ModelNode n : result.asList()) {
            strNames.add(n.asString());
        }


        assertTrue(strNames.contains("server"));
        assertTrue(strNames.contains("servlet-container"));
    }

    @Test
    public void testReadChildrenNames() throws Exception {

        ModelNode op = HttpMgmtProxy.getOpNode("subsystem=undertow", "read-children-names");
        op.get("child-type").set("server");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        List<ModelNode> names = result.asList();
        Set<String> strNames = new TreeSet<>();
        for (ModelNode n : names) { strNames.add(n.asString()); }

        assertTrue(strNames.contains("default-server"));

    }

    @Test
    public void testReadChildrenResources() throws Exception {

        ModelNode op = HttpMgmtProxy.getOpNode("subsystem=undertow", "read-children-resources");
        op.get("child-type").set("server");

        ModelNode ret = httpMgmt.sendPostCommand(op);
        assertTrue("success".equals(ret.get("outcome").asString()));

        ModelNode result = ret.get("result");

        assertTrue(result.has("default-server"));
    }

    @Test
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
        String cURL = "http://" + url.getHost() + ":8181";

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
