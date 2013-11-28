/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.ws;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Some tests on changes to the model that are applied immediately to the runtime
 * when there's no WS deployment on the server.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WSAttributesChangesTestCase {

    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String DEP_1 = "jaxws-manual-pojo-1";
    private static final String DEP_2 = "jaxws-manual-pojo-2";

    @ArquillianResource
    ContainerController containerController;

    @ArquillianResource
    Deployer deployer;
    
    @Deployment(name = DEP_1, testable = false, managed = false)
    public static WebArchive deployment1() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, DEP_1 + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
        return pojoWar;
    }

    @Deployment(name = DEP_2, testable = false, managed = false)
    public static WebArchive deployment2() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, DEP_2 + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
        return pojoWar;
    }

    @Before
    public void startContainer() throws Exception {
        containerController.start(DEFAULT_JBOSSAS);
    }

    @Test
    public void testWsdlHostChanges() throws Exception {
        performWsdlHostAttributeTest(false);
        performWsdlHostAttributeTest(true);
    }
    
    private void performWsdlHostAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        
        ModelControllerClient client = managementClient.getControllerClient();
        String initialWsdlHost = null;
        try {
            initialWsdlHost = getAttribute("wsdl-host", client);
            
            final String hostnameA = "foo-host-a";
            
            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
            op.get(VALUE).set(hostnameA);
            applyUpdate(client, op, false); //update successful, no need to reload
            
            //now we deploy an endpoint...
            deployer.deploy(DEP_1);

            //verify the updated wsdl host is used...
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEP_1 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostnameA); 
            
            if (checkUpdateWithDeployedEndpoint) {
                final String hostnameB = "foo-host-b";
                
                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-host");
                opB.get(VALUE).set(hostnameB);
                applyUpdate(client, opB, true); //update again, but we'll need to reload, as there's an active deployment
                
                //check the wsdl host is still the one we updated to before
                checkWsdl(wsdlURL, hostnameA);
                
                //and check that still applies even if we undeploy and redeploy the endpoint
                deployer.undeploy(DEP_1);
                deployer.deploy(DEP_1);
                checkWsdl(wsdlURL, hostnameA);
            }
        } finally {
            try {
                deployer.undeploy(DEP_1);
            } catch (Throwable t) {
                //ignore
            }
            try {
                if (initialWsdlHost != null) {
                    ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                    op.get(NAME).set("wsdl-host");
                    op.get(VALUE).set(initialWsdlHost);
                    applyUpdate(client, op, checkUpdateWithDeployedEndpoint);
                }
            } finally {
                managementClient.close();
            }
        }
    }
    
    @Test
    public void testWsdlPortChanges() throws Exception {
        performWsdlPortAttributeTest(false);
        performWsdlPortAttributeTest(true);
    }
    
    private void performWsdlPortAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        
        ModelControllerClient client = managementClient.getControllerClient();
        try {
            final String portA = "55667";
            
            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-port");
            op.get(VALUE).set(portA);
            applyUpdate(client, op, false); //update successful, no need to reload
            
            //now we deploy an endpoint...
            deployer.deploy(DEP_2);

            //verify the updated wsdl port is used...
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEP_2 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, portA); 
            
            if (checkUpdateWithDeployedEndpoint) {
                final String portB = "55668";
                
                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-port");
                opB.get(VALUE).set(portB);
                applyUpdate(client, opB, true); //update again, but we'll need to reload, as there's an active deployment
                
                //check the wsdl port is still the one we updated to before
                checkWsdl(wsdlURL, portA);
                
                //and check that still applies even if we undeploy and redeploy the endpoint
                deployer.undeploy(DEP_2);
                deployer.deploy(DEP_2);
                checkWsdl(wsdlURL, portA);
            }
        } finally {
            try {
                deployer.undeploy(DEP_2);
            } catch (Throwable t) {
                //ignore
            }
            try {
                ModelNode op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
                op.get(NAME).set("wsdl-port");
                applyUpdate(client, op, checkUpdateWithDeployedEndpoint);
            } finally {
                managementClient.close();
            }
        }
    }
    
    @After
    public void stopContainer() {
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            containerController.stop(DEFAULT_JBOSSAS);
        }
    }

    private String getAttribute(final String attribute, final ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=webservices/", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attribute);
        final ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.assertTrue(result.hasDefined(RESULT));
            return result.get(RESULT).asString();
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();
        // set address
        ModelNode list = op.get(ADDRESS).setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static ModelNode applyUpdate(final ModelControllerClient client, final ModelNode update, final boolean expectReloadRequired) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (expectReloadRequired) {
                Assert.assertTrue(result.hasDefined(RESPONSE_HEADERS));
                ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
                Assert.assertTrue(responseHeaders.hasDefined(OPERATION_REQUIRES_RELOAD));
                Assert.assertEquals("true", responseHeaders.get(OPERATION_REQUIRES_RELOAD).asString());
            } else {
                Assert.assertFalse(result.hasDefined(RESPONSE_HEADERS));
            }
            return result;
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    private void checkWsdl(URL wsdlURL, String hostOrPort) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(200, connection.getResponseCode());
            connection.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("address location")) {
                    Assert.assertTrue(line.contains(hostOrPort));
                    return;
                }
            }
            fail("Could not check soap:address!");
        } finally {
            connection.disconnect();
        }
    }
}
