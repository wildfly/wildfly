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
package org.jboss.as.test.manualmode.management.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GlobalOpsTestCase extends AbstractCliTestBase {

    public static final String DEFAULT_JBOSSAS = "default-jbossas";
    public static final String DEPLOYMENT_NAME = "DUMMY";
    
    @ArquillianResource
    private static ContainerController container;
    @ArquillianResource
    private Deployer deployer;
    
    //NOTE: BeforeClass is not subject to ARQ injection.
    @Before
    public void initServer() throws Exception {
        container.start(DEFAULT_JBOSSAS);
        initCLI();
    }

    @After
    public void closeServer() throws Exception {
        closeCLI();
        container.stop(DEFAULT_JBOSSAS);
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }


    @Test
    public void testPersistentRestart() throws Exception {
        //AS7-5929
        cli.sendLine(":server-set-restart-required");        
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        checkResponseHeadersForProcessState(result);
        cli.sendLine(":read-resource");
        assertTrue(result.isIsOutcomeSuccess());
        result = cli.readAllAsOpResult();
        checkResponseHeadersForProcessState(result);

        
        cli.sendLine(":reload");        
        assertTrue(result.isIsOutcomeSuccess());
        result = cli.readAllAsOpResult();
        assertNoProcessState(result);
        
        TimeUnit.SECONDS.sleep(10);
        cli.sendLine(":read-resource");        
        assertTrue(result.isIsOutcomeSuccess());
        result = cli.readAllAsOpResult();
        checkResponseHeadersForProcessState(result);
        
    }

    protected void checkResponseHeadersForProcessState(CLIOpResult result) {
        assertNotNull("No response headers!", result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS));
        Map responseHeaders = (Map) result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS);
        Object processState = responseHeaders.get("process-state");
        assertNotNull("No process state in response-headers!", processState);
        assertTrue("Process state is of wrong type!", processState instanceof String);
        assertEquals("Wrong content of process-state header", "restart-required", (String) processState);

    }
    
    protected void assertNoProcessState(CLIOpResult result) {
        if(result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS) == null){
            return;
        }
        Map responseHeaders = (Map) result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS);
        Object processState = responseHeaders.get("process-state");
        assertNull(processState);
        

    }
}
