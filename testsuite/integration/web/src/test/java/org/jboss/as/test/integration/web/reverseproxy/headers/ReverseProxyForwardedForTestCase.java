/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.reverseproxy.headers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReverseProxyForwardedForTestCase {

    @ContainerResource
    private ManagementClient managementClient;
    private static ManagementClient mc;

    @Before
    public void setup() throws Exception {
        if (mc == null) {
            mc = managementClient;
            //add the reverse proxy
            ModelNode op = new ModelNode();
            ModelNode addr = new ModelNode();
            addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
            addr.add("configuration", "handler");
            addr.add("reverse-proxy", "myproxy");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get("max-request-time").set(60000);
            op.get("connection-idle-timeout").set(60000);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            //add the hosts
            ModelNode addSocketBindingOp = getOutboundSocketBinding(managementClient.getWebUri().getHost(), managementClient.getWebUri().getPort());
            ManagementOperations.executeOperation(managementClient.getControllerClient(),addSocketBindingOp);

            op = new ModelNode();
            addr = new ModelNode();
            addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
            addr.add("configuration", "handler");
            addr.add("reverse-proxy", "myproxy");
            addr.add("host", "server1");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get("outbound-socket-binding").set("proxy-host");
            op.get("path").set("/server1");
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
            op = new ModelNode();
            addr = new ModelNode();
            addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
            addr.add("server", "default-server");
            addr.add("host", "default-host");
            addr.add("location", "/proxy");
            op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
            op.get("handler").set("myproxy");
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }

    private static ModelNode getOutboundSocketBinding(String address, int port) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy-host", "add");
        op.get("host").set(address);
        op.get("port").set(port);
        return op;
    }

    @AfterClass
    public static void tearDown() throws Exception {

        ModelNode op = new ModelNode();
        ModelNode addr = new ModelNode();

        addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        addr.add("configuration", "handler");
        addr.add("reverse-proxy", "myproxy");
        addr.add("host", "server1");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(mc.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        addr.add("server", "default-server");
        addr.add("host", "default-host");
        addr.add("location", "/proxy");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(mc.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        addr.add("configuration", "handler");
        addr.add("reverse-proxy", "myproxy");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(mc.getControllerClient(), op);

        op = createOpNode("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy-host", "remove");
        ManagementOperations.executeOperation(mc.getControllerClient(), op);
    }

    @ArquillianResource
    @OperateOnDeployment("server1")
    private URL url;

    @Deployment(name = "server1", testable = false)
    public static WebArchive server1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "server1.war");
        war.addClasses(ServerNameServlet.class);
        war.addAsWebInfResource(ReverseProxyForwardedForTestCase.class.getPackage(), "web-server1.xml", "web.xml");
        return war;
    }

    private String performCall(CloseableHttpClient httpclient, String urlPattern) throws Exception {
        HttpGet get = new HttpGet("http://" + url.getHost() + ":" + url.getPort() + "/proxy/" + urlPattern);
        get.addHeader("X-Forwarded-For", "1.1.1.1");
        HttpResponse res =  httpclient.execute(get);
        Assert.assertEquals(200, res.getStatusLine().getStatusCode());
        return EntityUtils.toString(res.getEntity());
    }

    @Test
    public void testNoForwarded() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            setReuseForawrdedTo(false);// thats default
            final String result = performCall(httpclient, "name?header=X_Forwarded_For");
            Assert.assertEquals(url.getHost(), result);
        }
    }

    @Test
    public void testWithForwarded() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            setReuseForawrdedTo(true);
            final String result = performCall(httpclient, "name?header=X_Forwarded_For");
            Assert.assertEquals("1.1.1.1,"+url.getHost(), result);
        } finally {
            setReuseForawrdedTo(false);
        }
    }

    private void setReuseForawrdedTo(final boolean value) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        addr.add("configuration", "handler");
        addr.add("reverse-proxy", "myproxy");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set("reuse-x-forwarded-header");
        op.get(ModelDescriptionConstants.VALUE).set(value);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    public void testReverseProxyMaxRequestTime2() throws Exception {
        // set the max-request-time to a lower value than the wait
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("http://" + url.getHost() + ":" + url.getPort() + "/proxy/name?wait=50");
            get.addHeader("X-Forwarded-For", "1.1.1.1");
            HttpResponse res = httpclient.execute(get);
            // With https://issues.redhat.com/browse/UNDERTOW-1459 fix, status code should be 504
            // FIXME: after undertow 2.2.13.Final integrated into WildFly, this should be updated to 504 only
            Assert.assertTrue("Service Unaviable expected because max-request-time is set to 10ms", res.getStatusLine().getStatusCode() == 504 || res.getStatusLine().getStatusCode() == 503);
        }
    }
}
