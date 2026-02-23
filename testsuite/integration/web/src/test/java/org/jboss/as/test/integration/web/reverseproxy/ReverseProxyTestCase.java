/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.reverseproxy;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
public class ReverseProxyTestCase {

    @ContainerResource
    private ManagementClient managementClient;
    private static ManagementClient mc;
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
        addr.add("configuration", "handler");
        addr.add("reverse-proxy", "myproxy");
        addr.add("host", "server2");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get("outbound-socket-binding").set("proxy-host");
        op.get("path").set("/server2");

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

    private static ModelNode getOutboundSocketBinding(String address, int port) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy-host", "add");
        op.get("host").set(address);
        op.get("port").set(port);
        return op;
    }


    @After
    public void tearDown() throws Exception {
        serverSnapshot.close();
    }

    @ArquillianResource
    @OperateOnDeployment("server1")
    private URL url;

    @Deployment(name = "server1", testable = false)
    public static WebArchive server1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "server1.war");
        war.addClasses(ServerNameServlet.class, CookieListener.class);
        war.addAsWebInfResource(ReverseProxyTestCase.class.getPackage(), "web-server1.xml", "web.xml");
        return war;
    }

    @Deployment(name = "server2", testable = false)
    public static WebArchive server2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "server2.war");
        war.addClasses(ServerNameServlet.class, CookieListener.class);
        war.addAsWebInfResource(ReverseProxyTestCase.class.getPackage(), "web-server2.xml", "web.xml");
        return war;
    }


    private String performCall(CloseableHttpClient httpclient, String urlPattern) throws Exception {
        HttpResponse res =  httpclient.execute(new HttpGet("http://" + url.getHost() + ":" + url.getPort() + "/proxy/" + urlPattern));
        Assert.assertEquals(200, res.getStatusLine().getStatusCode());
        return EntityUtils.toString(res.getEntity());
    }

    @Test
    public void testReverseProxy() throws Exception {

        try (CloseableHttpClient httpclient =  HttpClients.createDefault()){
            final Set<String> results = new HashSet<>();
            for (int i = 0; i < 10; ++i) {
                results.add(performCall(httpclient,"name"));
            }
            Assert.assertEquals(2, results.size());
            Assert.assertTrue(results.contains("server1"));
            Assert.assertTrue(results.contains("server2"));
            //TODO: re-add JVM route based sticky session testing
            //String session = performCall("name?session=true");
            //sticky sessions should stick it to this node
            //for (int i = 0; i < 10; ++i) {
            //    Assert.assertEquals(session, performCall("name"));
            //}
        }
    }

    private void configureMaxRequestTime(int value) throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        op.get(ModelDescriptionConstants.OP_ADDR).add("configuration", "handler");
        op.get(ModelDescriptionConstants.OP_ADDR).add("reverse-proxy", "myproxy");
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set("max-request-time");
        op.get(ModelDescriptionConstants.VALUE).set(value);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        ServerReload.reloadIfRequired(managementClient);
    }

    @Test
    public void testReverseProxyMaxRequestTime() throws Exception {
        // set the max-request-time to a lower value than the wait
        configureMaxRequestTime(10);
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse res = httpclient.execute(new HttpGet("http://" + url.getHost() + ":" + url.getPort() + "/proxy/name?wait=50"));
            // With https://issues.redhat.com/browse/UNDERTOW-1459 fix, status code should be 504
            // FIXME: after undertow 2.2.13.Final integrated into WildFly, this should be updated to 504 only
            Assert.assertTrue("Service Unaviable expected because max-request-time is set to 10ms", res.getStatusLine().getStatusCode() == 504 || res.getStatusLine().getStatusCode() == 503);
        }
    }
}
