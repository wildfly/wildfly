/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.reverseproxy.headers;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;
import java.net.InetAddress;
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
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
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
public class ReverseProxyRewriteHostTestCase {

    @ContainerResource
    private ManagementClient managementClient;
    private AutoCloseable serverSnapshot;

    @Before
    public void setup() throws Exception {
        serverSnapshot = ServerSnapshot.takeSnapshot(managementClient);
        // add the reverse proxy
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

        // add the hosts
        ModelNode addSocketBindingOp = getOutboundSocketBinding(managementClient.getWebUri().getHost(),
                managementClient.getWebUri().getPort());
        ManagementOperations.executeOperation(managementClient.getControllerClient(), addSocketBindingOp);

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

    @After
    public void tearDown() throws Exception {
        serverSnapshot.close();
    }

    private static ModelNode getOutboundSocketBinding(String address, int port) {
        ModelNode op = createOpNode(
                "socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy-host", "add");
        op.get("host").set(address);
        op.get("port").set(port);
        return op;
    }

    @ArquillianResource
    @OperateOnDeployment("server1")
    private URL url;

    @Deployment(name = "server1", testable = false)
    public static WebArchive server1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "server1.war");
        war.addClasses(ServerNameServlet.class);
        war.addAsWebInfResource(ReverseProxyRewriteHostTestCase.class.getPackage(), "web-server1.xml", "web.xml");
        return war;
    }

    private String performCall(CloseableHttpClient httpclient, String urlPattern) throws Exception {
        HttpGet get = new HttpGet("http://" + url.getHost() + ":" + url.getPort() + "/proxy/" + urlPattern);
        HttpResponse res =  httpclient.execute(get);
        Assert.assertEquals(200, res.getStatusLine().getStatusCode());
        return EntityUtils.toString(res.getEntity());
    }

    @Test
    public void testNoRewrite() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            setReuseForwardedTo(false);// thats default
            final String result = performCall(httpclient, "name?header=X_Forwarded_Host"); //this is side effect, but Host header is localhost in both, so checking it makes sense
            final InetAddress resultAddress = InetAddress.getByName(result);
            final InetAddress hostAddress = InetAddress.getByName(url.getHost());
            Assert.assertEquals(hostAddress, resultAddress);
        }
    }

    @Test
    public void testWithRewrite() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            setReuseForwardedTo(true);
            final String result = performCall(httpclient, "name?header=X_Forwarded_Host");
            final int lastInd = result.lastIndexOf(":"); //ipv6 will have more :, we cant split on it.
            Assert.assertNotEquals(-1, lastInd);
            final String[] parts = new String[] {result.substring(0,lastInd), result.substring(lastInd+1)};
            final InetAddress resultAddress = InetAddress.getByName(parts[0]);
            final InetAddress hostAddress = InetAddress.getByName(url.getHost());
            Assert.assertEquals(hostAddress.toString(), resultAddress.toString());
            Assert.assertEquals(url.getPort()+"", parts[1]);
        } finally {
            setReuseForwardedTo(false);
        }
    }

    private void setReuseForwardedTo(final boolean value) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        addr.add("configuration", "handler");
        addr.add("reverse-proxy", "myproxy");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set("rewrite-host-header");
        op.get(ModelDescriptionConstants.VALUE).set(value);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

}
