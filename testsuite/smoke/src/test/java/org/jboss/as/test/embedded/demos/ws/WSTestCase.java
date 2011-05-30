/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.embedded.demos.ws;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.NewModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.demos.ws.archive.Endpoint;
import org.jboss.as.demos.ws.archive.EndpointImpl;
import org.jboss.as.test.modular.utils.PollingUtils;
import org.jboss.as.test.modular.utils.PollingUtils.UrlConnectionTask;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.as.webservices.dmr.WSExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WSTestCase {

    @Deployment(testable = false)
    public static Archive<?> getDeployment(){
        return ShrinkWrapUtils.createWebArchive("demos/ws-example.war", EndpointImpl.class.getPackage());
    }

    @Test
    public void testWSDL() throws Exception {
        String s = performCall("?wsdl", null);
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("wsdl:definitions"));
    }

    @Test
    public void testManagementDescription() throws Exception {
        final NewModelControllerClient client = NewModelControllerClient.Factory.create("localhost", 9999);
        try {
            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.DEPLOYMENT, "ws-example.war");
            address.add(ModelDescriptionConstants.SUBSYSTEM, WSExtension.SUBSYSTEM_NAME); //EndpointService
            address.add("endpoint", "*"); // get all endpoints

            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
            operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
            operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

            final ModelNode result = client.execute(operation);
            Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

            for(final ModelNode endpointResult : result.get("result").asList()) {
                final ModelNode endpoint = endpointResult.get("result");
                Assert.assertTrue(endpoint.hasDefined("class"));
                Assert.assertTrue(endpoint.hasDefined("name"));
                Assert.assertTrue(endpoint.hasDefined("wsdl-url"));

                final URL url = new URL(endpoint.get("wsdl-url").asString());
                UrlConnectionTask task = new UrlConnectionTask(url, null);
                PollingUtils.retryWithTimeout(10000, task);

                // Read a metric
                final ModelNode readAttribute = new ModelNode();
                readAttribute.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
                readAttribute.get(ModelDescriptionConstants.OP_ADDR).set(endpointResult.get(ModelDescriptionConstants.OP_ADDR));
                readAttribute.get(ModelDescriptionConstants.NAME).set("request-count");

                final ModelNode attribute = client.execute(operation);
                Assert.assertEquals(ModelDescriptionConstants.SUCCESS, attribute.get(ModelDescriptionConstants.OUTCOME).asString());
                Assert.assertTrue(attribute.get("result").asInt() > 0);
            }

        } finally {
            client.close();
        }
    }

    @Test
    @Ignore("[AS7-814] Fix or remove ignored smoke tests")
    public void testAccess() throws Exception {
        URL wsdlURL = new URL("http://localhost:8080/ws-example?wsdl");
        QName serviceName = new QName("http://archive.ws.demos.as.jboss.org/", "EndpointService");
        Service service = Service.create(wsdlURL, serviceName);
        Endpoint port = (Endpoint) service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }


    private static String performCall(String params, String request) throws Exception {
        URL url = new URL("http://localhost:8080/ws-example/" + params);
        UrlConnectionTask task = new UrlConnectionTask(url, request);
        PollingUtils.retryWithTimeout(10000, task);
        return task.getResponse();
    }
}
