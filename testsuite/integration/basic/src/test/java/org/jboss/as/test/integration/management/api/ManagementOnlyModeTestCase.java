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
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.Callable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
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
import org.junit.Assert;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import org.jboss.as.test.integration.management.base.ArquillianResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.junit.Ignore;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("ARQ-791")
public class ManagementOnlyModeTestCase extends ArquillianResourceMgmtTestBase {

    @ArquillianResource
    URL url;
    private static final int TEST_PORT = 9091;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }
    
    @Test
    public void testManagementOnlyMode() throws Exception {
                
        // restart server to management-only mode
        ModelNode op = createOpNode(null, "reload");        
        op.get(ADMIN_ONLY).set(true);        
        ModelNode result = executeOperation(op);
        
        // wait until the server is admin-only mode        
        RetryTaskExecutor rte = new RetryTaskExecutor();
        rte.retryTask(new Callable<ModelNode>() {

            public ModelNode call() throws Exception {
                ModelNode rop = createOpNode(null, READ_ATTRIBUTE_OPERATION);        
                rop.get(NAME).set("running-mode");        
                ModelNode mode = executeOperation(rop);
                if (! mode.asString().equals("ADMIN_ONLY")) throw new Exception ("Wrong mode.");
                return mode;
            }
        });
        
        // check that the server is unreachable
        Assert.assertFalse("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", url.getHost(), url.getPort(), "/").toString()));
        
        // update the model in admin-only mode - add a web connector
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", ADD);            
        op.get("interface").set("public");        
        op.get("port").set(TEST_PORT);        
        result = executeOperation(op);
                       
        op = createOpNode("subsystem=web/connector=test", ADD);    
        op.get("socket-binding").set("test-binding");
        op.get("protocol").set("HTTP/1.1");
        op.get("scheme").set("http");
        result = executeOperation(op);        
                
        // restart the server back to normal mode
        op = createOpNode(null, "reload");        
        op.get(ADMIN_ONLY).set(false);        
        result = executeOperation(op);        

        // wait until the server is in normal mode        
        rte = new RetryTaskExecutor();
        rte.retryTask(new Callable<ModelNode>() {
            public ModelNode call() throws Exception {
                ModelNode rop = createOpNode(null, READ_ATTRIBUTE_OPERATION);        
                rop.get(NAME).set("running-mode");        
                ModelNode mode = executeOperation(rop);
                if (! mode.asString().equals("NORMAL")) throw new Exception ("Wrong mode.");
                return mode;
            }
        });

        
        // check that the server is up
        Assert.assertTrue("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", url.getHost(), url.getPort(), "/").toString()));
        
        // check that the changes made in admin-only mode have been applied - test the connector
        Assert.assertTrue("Could not connect to created connector.",WebUtil.testHttpURL(new URL(
                "http", url.getHost(), TEST_PORT, "/").toString()));
        
        // remove the conector
        op = createOpNode("subsystem=web/connector=test", REMOVE);    
        result = executeOperation(op);        
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-binding", REMOVE);            
        result = executeOperation(op);
    }
}
