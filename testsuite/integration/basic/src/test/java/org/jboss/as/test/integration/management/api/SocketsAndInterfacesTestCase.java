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
package org.jboss.as.test.integration.management.api;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SocketsAndInterfacesTestCase extends AbstractMgmtTestBase {
    
    private static final Logger log = Logger.getLogger(SocketsAndInterfacesTestCase.class);
    
    @ArquillianResource
    URL url;
    private NetworkInterface testNic;
    private static final int TEST_PORT = 9091;
    
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }
    
    @Before
    public void before() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
        testNic = getNonDefaultNic();
    }

    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }
    
    @Test
    public void testAddUpdateRemove() throws Exception {

        if (testNic == null) {
            log.error("Could not look up non-default interface");
            return;
        }
        
        // add interface
        ModelNode op = createOpNode("interface=test-interface", ADD);    
        op.get("nic").set(testNic.getName());        
        ModelNode result = executeOperation(op);        
        
        // add socket binding using created interface
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", ADD);            
        op.get("interface").set("test-interface");        
        op.get("port").set(TEST_PORT);        
        result = executeOperation(op);
        
        
        // add a web connector so we can test the interface
        op = createOpNode("subsystem=web/connector=test", ADD);    
        op.get("socket-binding").set("test-binding");
        op.get("protocol").set("HTTP/1.1");
        op.get("scheme").set("http");
        result = executeOperation(op);        
        
        // test the connector
        String testHost = testNic.getInetAddresses().nextElement().getHostName();
        Assert.assertTrue("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", testHost, TEST_PORT, "/").toString()));
        
        // change socket binding port
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", WRITE_ATTRIBUTE_OPERATION);            
        op.get(NAME).set("port");
        op.get(VALUE).set(TEST_PORT + 1);        
        result = executeOperation(op, false);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.get(RESPONSE_HEADERS).get(PROCESS_STATE).asString().equals("reload-required"));
        
        // reload server
        op = createOpNode(null, "reload");
        result = executeOperation(op);                
        Thread.sleep(5000);
        
        // check the connector is not listening on the old port
        Assert.assertFalse("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", testHost, TEST_PORT, "/").toString()));
        // check the connector is listening on the new port
        Assert.assertTrue("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", testHost, TEST_PORT + 1, "/").toString()));
        
        // try to remove the interface while the socket binding is still  bound to it - should fail
        op = createOpNode("interface=test-interface", REMOVE);    
        result = executeOperation(op, false);
        Assert.assertFalse("Removed interface with socket binding bound to it.", SUCCESS.equals(result.get(OUTCOME).asString()));
        
        // try to remove socket binding while the connector is still using it - should fail
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", REMOVE);            
        result = executeOperation(op, false);
        Assert.assertFalse("Removed socked binding with connector still using it.", SUCCESS.equals(result.get(OUTCOME).asString()));        
        
        // remove connector
        op = createOpNode("subsystem=web/connector=test", REMOVE);    
        result = executeOperation(op);          
        
        // check that the connector is down
        Assert.assertFalse("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", testHost, TEST_PORT, "/").toString()));
        
        // remove socket binding
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", REMOVE);            
        result = executeOperation(op);

        // remove interface
        op = createOpNode("interface=test-interface", REMOVE);    
        result = executeOperation(op);        
        
    }
        
    private NetworkInterface getNonDefaultNic() throws SocketException, UnknownHostException {
        
        InetAddress defaultAddr = InetAddress.getByName(url.getHost());
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            if (! nic.isUp()) continue;
            for (InterfaceAddress addr : nic.getInterfaceAddresses()) {                
                if (addr.getAddress().equals(defaultAddr)) continue;
            }
            // interface found
            return nic;
        }
        return null; // no interface found
    }
    
}
