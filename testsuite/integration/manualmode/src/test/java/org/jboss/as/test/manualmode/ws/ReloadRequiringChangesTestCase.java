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
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.ServerReload;
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
 * Some tests on changes to the model requiring reload
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReloadRequiringChangesTestCase {

    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String DEPLOYMENT = "jaxws-manual-pojo";

    @ArquillianResource
    ContainerController containerController;

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = DEPLOYMENT, testable = false, managed = false)
    public static WebArchive deployment() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
        return pojoWar;
    }

    @Before
    public void startContainer() throws Exception {
        containerController.start(DEFAULT_JBOSSAS);
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            deployer.deploy(DEPLOYMENT);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testWSDLHostChangeRequiresReloadAndDoesNotAffectRuntime() throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

        ModelControllerClient client = managementClient.getControllerClient();
        String initialWsdlHost = null;
        try {
            initialWsdlHost = getWsdlHost(client);

            //change wsdl-host to "foo-host" and reload
            final String hostname = "foo-host";
            setWsdlHost(client, hostname);
            ServerReload.executeReloadAndWaitForCompletion(client);

            //change wsdl-host to "bar-host" and verify deployment still uses "foo-host"
            setWsdlHost(client, "bar-host");
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEPLOYMENT + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostname);
        } finally {
            try {
                if (initialWsdlHost != null) {
                    setWsdlHost(client, initialWsdlHost);
                }
            } finally {
                managementClient.close();
            }
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testWSDLHostUndefineRequiresReloadAndDoesNotAffectRuntime() throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

        ModelControllerClient client = managementClient.getControllerClient();
        String initialWsdlHost = null;
        try {
            initialWsdlHost = getWsdlHost(client);

            //change wsdl-host to "my-host" and reload
            final String hostname = "my-host";
            setWsdlHost(client, hostname);
            ServerReload.executeReloadAndWaitForCompletion(client);

            //undefine wsdl-host and verify deployment still uses "foo-host"
            setWsdlHost(client, null);
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEPLOYMENT + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostname);
        } finally {
            try {
                if (initialWsdlHost != null) {
                    setWsdlHost(client, initialWsdlHost);
                }
            } finally {
                managementClient.close();
            }
        }
    }

    @After
    public void stopContainer() {
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            deployer.undeploy(DEPLOYMENT);
        }
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            containerController.stop(DEFAULT_JBOSSAS);
        }
    }

    private String getWsdlHost(final ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=webservices/", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set("wsdl-host");
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

    private void setWsdlHost(final ModelControllerClient client, final String wsdlHost) throws Exception {
        ModelNode op;
        if (wsdlHost != null) {
            op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
            op.get(VALUE).set(wsdlHost);
        } else {
            op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
        }
        applyUpdate(client, op);
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

    private static ModelNode applyUpdate(final ModelControllerClient client, final ModelNode update) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.assertTrue(result.hasDefined(RESPONSE_HEADERS));
            ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
            Assert.assertTrue(responseHeaders.hasDefined(OPERATION_REQUIRES_RELOAD));
            Assert.assertEquals("true", responseHeaders.get(OPERATION_REQUIRES_RELOAD).asString());
            return result;
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }


    private void checkWsdl(URL wsdlURL, String host) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(200, connection.getResponseCode());
            connection.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("address location")) {
                    Assert.assertTrue(line.contains(host));
                    return;
                }
            }
            fail("Could not check soap:address!");
        } finally {
            connection.disconnect();
        }
    }
}
