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

import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * 
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>  (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReloadOpsTestCase extends AbstractCliTestBase {

    public static final String DEFAULT_JBOSSAS = "default-jbossas";
    public static final String DEPLOYMENT_NAME = "DUMMY";
    private static final long TIMEOUT = 100000L;
    
    @ArquillianResource
    private static ContainerController container;
    
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
        ja.addClass(ReloadOpsTestCase.class);
        return ja;
    }

    @Test
    public void testWriteAttribvuteWithReload() throws Exception {
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        cli.sendLine("/subsystem=jpa:read-attribute(name=default-extended-persistence-inheritance)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        String value = (String) result.getResult();
        assertThat(value, is("DEEP"));
        cli.sendLine("/subsystem=jpa:write-attribute(name=default-extended-persistence-inheritance, value=SHALLOW)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        checkResponseHeadersForProcessState(result);
        reloadServer(managementClient, TIMEOUT);
        cli.sendLine("/subsystem=jpa:read-attribute(name=default-extended-persistence-inheritance)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        value = (String) result.getResult();
        assertThat(value, is("SHALLOW"));
        cli.sendLine("/subsystem=jpa:write-attribute(name=default-extended-persistence-inheritance, value=SHALLOW)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        assertNoProcessState(result);
        cli.sendLine("/subsystem=jpa:read-attribute(name=default-extended-persistence-inheritance)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        value = (String) result.getResult();
        assertThat(value, is("SHALLOW"));
    }
    
    private void reloadServer(ManagementClient managementClient, long timeout) throws Exception {
        executeReload(managementClient.getControllerClient());
        waitForLiveServerToReload(timeout);
    }

     private void executeReload(ModelControllerClient client) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        try {
            Assert.assertTrue(Operations.isSuccessfulOutcome(client.execute(operation)));
        } catch (IOException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof ExecutionException) {
                // ignore, this might happen if the channel gets closed before we got the response
            } else {
                throw e;
            }
        } finally {
            client.close();
        }
    }

    private void waitForLiveServerToReload(long timeout) throws Exception {
        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < timeout) {
            ModelControllerClient liveClient = ModelControllerClient.Factory.create(
                    TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            try {
                ModelNode result = liveClient.execute(operation);
                if ("running".equals(result.get(RESULT).asString())) {
                    return;
                }
            } catch (IOException e) {
            } finally {
                IoUtils.safeClose(liveClient);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        fail("Live Server did not reload in the imparted time.");
    }

    protected void checkResponseHeadersForProcessState(CLIOpResult result) {
        assertNotNull("No response headers!", result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS));
        Map responseHeaders = (Map) result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS);
        Object processState = responseHeaders.get("process-state");
        assertNotNull("No process state in response-headers!", processState);
        assertTrue("Process state is of wrong type!", processState instanceof String);
        assertEquals("Wrong content of process-state header", "reload-required", (String) processState);

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