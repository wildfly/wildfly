/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.After;
import org.junit.Assert;
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
public class ReloadWSDLPublisherTestCase {

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
    public void endpointLookup() throws Exception {
        containerController.start(DEFAULT_JBOSSAS);
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            deployer.deploy(DEPLOYMENT);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testHelloStringAfterReload() throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        QName serviceName = new QName("http://jbossws.org/basic", "POJOService");
        URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEPLOYMENT + "/POJOService?wsdl");
        checkWsdl(wsdlURL);
        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!", proxy.helloString("World"));
        reloadServer(managementClient, 100000);
        checkWsdl(wsdlURL);
        serviceName = new QName("http://jbossws.org/basic", "POJOService");
        service = Service.create(wsdlURL, serviceName);
        proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!", proxy.helloString("World"));
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
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

    private void reloadServer(ManagementClient managementClient, int timeout) throws Exception {
        executeReload(managementClient.getControllerClient());
        waitForLiveServerToReload(timeout);
    }

    private void executeReload(ModelControllerClient client) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        try {
            ModelNode result = client.execute(operation);
            Assert.assertEquals("success", result.get(ClientConstants.OUTCOME).asString());
        } catch(IOException e) {
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

    private void waitForLiveServerToReload(int timeout) throws Exception {
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

    private void checkWsdl(URL wsdlURL) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(200, connection.getResponseCode());
        } finally {
            connection.disconnect();
        }
    }
}
