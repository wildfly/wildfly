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
package org.jboss.as.test.integration.management.api.web;

import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import java.util.concurrent.TimeUnit;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import java.util.logging.Logger;
import org.junit.Before;
import java.io.IOException;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.api.AbstractMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VirtualServerTestCase extends AbstractMgmtTestBase {
    
    @ArquillianResource
    static URL url;
    
    private static String defaultHost;
    private static String virtualHost;
    private static final Logger log = Logger.getLogger(VirtualServerTestCase.class.getName());
    
    @Deployment(order=1)
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }
    
    @Deployment(managed=false, name="vsdeployment", order=2)
    public static Archive<?> getVDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "vsDeployment.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebResource(new StringAsset("Virtual Server Deployment"), "index.html");
        war.addAsWebResource(new StringAsset("Rewrite Test"), "/rewritten/index.html");
        war.addAsWebInfResource(new StringAsset("<jboss-web><virtual-host>test</virtual-host></jboss-web>"), "jboss-web.xml");
        return war;        
    }    
    
    @Before
    public void before() throws IOException {
        super.init(url.getHost(), MGMT_PORT);                
    }        
    
    @Test
    public void testDefaultVirtualServer() throws IOException {
        
        // get default VS
        ModelNode result = executeOperation(createOpNode("subsystem=web/virtual-server=default-host", "read-resource"));
        
        // check VS
        assertTrue(result.get("alias").isDefined());
        assertTrue(result.get("default-web-module").asString().equals("ROOT.war"));        
    }
    
    @Test
    public void addRemoveVirtualServer(@ArquillianResource Deployer deployer) throws Exception {
       
        if (! resolveHosts()) {
            log.info("Unable to resolve alternate server host name.");
            return;
        }
        addVirtualServer();
        
        // deploy to virtual server
        deployer.deploy("vsdeployment");
        
        // check the deployment is available on and only on virtual server
        URL vURL = new URL(url.getProtocol(), virtualHost, url.getPort(), "/vsDeployment/index.html");
        String response = HttpRequest.get(vURL.toString(), 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("Virtual Server Deployment") >=0);
        
        URL dURL = new URL(url.getProtocol(), defaultHost, url.getPort(), "/vsDeployment/index.html");
        boolean failed = false;
        try {
            response = HttpRequest.get(dURL.toString(), 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
        }
        assertTrue("Deployment also on defaul server. " , failed);
        
        // undeploy form virtual server
        deployer.undeploy("vsdeployment");
        
        // remove virtual server
        removeVirtualServer();
        
    }
    
    private void addVirtualServer() throws IOException {        
        ModelNode addOp = createOpNode("subsystem=web/virtual-server=test", "add");
        addOp.get("alias").add(virtualHost);
        
        ModelNode rewrite = new ModelNode();
        rewrite.get("condition").setEmptyList();
        rewrite.get("pattern").set("toberewritten");
        rewrite.get("substitution").set("rewritten");
        rewrite.get("flags").set("nocase");
        addOp.get("rewrite").add(rewrite);
        
        executeOperation(addOp);
                
    }

    private void removeVirtualServer() throws IOException {        

        executeOperation(createOpNode("subsystem=web/virtual-server=test", "remove")); 
        
    }
    
    private boolean resolveHosts() {
        if (url.getHost().equals("localhost") || (url.getHost().equals("127.0.0.1"))) {
            defaultHost = "localhost";
            virtualHost = "127.0.0.1";
            return true;
        }
        
        return false;
    }
    
}
