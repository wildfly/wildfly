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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
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
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
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
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

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
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

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

    @Test
    public void testWsdlUriSchemeChanges() throws Exception {
        performWsdlUriSchemeAttributeTest(false);
        performWsdlUriSchemeAttributeTest(true);
    }

    private void performWsdlUriSchemeAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

        ModelControllerClient client = managementClient.getControllerClient();
        String initialWsdlUriScheme = null;
        try {
            //save initial wsdl-uri-schema value to restore later
            initialWsdlUriScheme = getAttribute("wsdl-uri-scheme", client, false);
            //set wsdl-uri-scheme value to https
            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-uri-scheme");
            op.get(VALUE).set("https");
            applyUpdate(client, op, false);
            deployer.deploy(DEP_1);
            //check if it works for the deployed endpoint url
            checkWSDLUriScheme(client, DEP_1 + ".war", "https");
            deployer.undeploy(DEP_1);

            //set wsdl-uri-scheme value to http
            ModelNode op2 = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op2.get(NAME).set("wsdl-uri-scheme");
            op2.get(VALUE).set("http");
            applyUpdate(client, op2, false);
            deployer.deploy(DEP_1);
            //check if the uri scheme of soap address is modified to http
            checkWSDLUriScheme(client, DEP_1 + ".war", "http");
            if (checkUpdateWithDeployedEndpoint) {
                //set wsdl-uri-schema value to http
                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-uri-scheme");
                opB.get(VALUE).set("https");
                applyUpdate(client, opB, true);
                //check this doesn't apply to endpointed which are deployed before this change
                checkWSDLUriScheme(client, DEP_1 + ".war", "http");
                deployer.undeploy(DEP_1);
                deployer.deploy(DEP_1);
                //check this will take effect to redeployed endpoint
                checkWSDLUriScheme(client, DEP_1 + ".war", "http");
            }
        } finally {
            try {
                deployer.undeploy(DEP_1);
            } catch (Throwable t) {
                //ignore
            }
            try {
                //restore the value of wsdl-uri-scheme attribute
                ModelNode op = null;
                if ("undefined".equals(initialWsdlUriScheme)) {
                    op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
                    op.get(NAME).set("wsdl-uri-scheme");
                } else {
                    op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                    op.get(NAME).set("wsdl-uri-scheme");
                    op.get(VALUE).set(initialWsdlUriScheme);
                }
                applyUpdate(client, op, checkUpdateWithDeployedEndpoint);
            } finally {
                managementClient.close();
            }
        }
    }

    @Test
    public void testWsdlPathRewriteRuleChanges() throws Exception {
        performWsdlPathRewriteRuleAttributeTest(false);
        performWsdlPathRewriteRuleAttributeTest(true);
    }


    private void performWsdlPathRewriteRuleAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

        ModelControllerClient client = managementClient.getControllerClient();

        try {

            final String expectedContext = "xx/jaxws-manual-pojo-1";
            final String sedCmdA = "s/jaxws-manual-pojo-1/xx\\/jaxws-manual-pojo-1/g";

            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-path-rewrite-rule");
            op.get(VALUE).set(sedCmdA);
            applyUpdate(client, op, false); //update successful, no need to reload

            //now we deploy an endpoint...
            deployer.deploy(DEP_1);

            //verify the updated wsdl host is used...
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEP_1 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, expectedContext);

            if (checkUpdateWithDeployedEndpoint) {
                //final String hostnameB = "foo-host-b";
                final String sedCmdB = "s/jaxws-manual-pojo-1/FOO\\/jaxws-manual-pojo-1/g";

                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-path-rewrite-rule");
                opB.get(VALUE).set(sedCmdB);
                applyUpdate(client, opB, true); //update again, but we'll need to reload, as there's an active deployment

                //check the wsdl host is still the one we updated to before
                checkWsdl(wsdlURL, expectedContext);

                //and check that still applies even if we undeploy and redeploy the endpoint
                deployer.undeploy(DEP_1);
                deployer.deploy(DEP_1);
                checkWsdl(wsdlURL, expectedContext);
            }
        } finally {
            try {
                deployer.undeploy(DEP_1);
            } catch (Throwable t) {
                //ignore
            }
            try {
                ModelNode op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
                op.get(NAME).set("wsdl-path-rewrite-rule");
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
        return getAttribute(attribute, client, true);
    }

    private String getAttribute(final String attribute, final ModelControllerClient client, final boolean checkDefined) throws Exception {
        ModelNode op = createOpNode("subsystem=webservices/", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attribute);
        final ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (checkDefined) {
                Assert.assertTrue(result.hasDefined(RESULT));
            }
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

    private void checkWSDLUriScheme(final ModelControllerClient managementClient, String deploymentName, String expectedScheme) throws Exception {
        final ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName);
        address.add(SUBSYSTEM, "webservices");
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(RECURSIVE).set(true);

        ModelNode result = managementClient.execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            final URL wsdlURL = new URL(endpoint.get("wsdl-url").asString());
            HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
            try {
                connection.connect();
                Assert.assertEquals(200, connection.getResponseCode());
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("address location")) {
                        if ("https".equals(expectedScheme)) {
                            Assert.assertTrue(line, line.contains("https"));
                            return;
                        } else {
                            Assert.assertTrue(line, line.contains("http") && !line.contains("https"));
                            return;
                        }
                    }
                }
                fail(line + " Could not check soap:address!");
            } finally {
                connection.disconnect();
            }
        }
    }
}
