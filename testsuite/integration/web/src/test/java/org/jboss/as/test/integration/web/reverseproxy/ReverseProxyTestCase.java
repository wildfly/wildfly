/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.reverseproxy;

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
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReverseProxyTestCase {

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
        addr.add("host", "server2");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ManagementOperations.executeOperation(mc.getControllerClient(), op);

        op = new ModelNode();
        addr = new ModelNode();
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
}
