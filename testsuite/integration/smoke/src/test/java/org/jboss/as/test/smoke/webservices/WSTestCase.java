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
package org.jboss.as.test.smoke.webservices;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.webservices.dmr.WSExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WSTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-example.war");
        war.addPackage(WSTestCase.class.getPackage());
        war.setWebXML(WSTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void testWSDL() throws Exception {
        String s = performCall("?wsdl");
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("wsdl:definitions"));
    }

    @Test
    public void testManagementDescription() throws Exception {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.DEPLOYMENT, "ws-example.war");
        address.add(ModelDescriptionConstants.SUBSYSTEM, WSExtension.SUBSYSTEM_NAME); //EndpointService
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        final ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            Assert.assertTrue(endpoint.hasDefined("class"));
            Assert.assertTrue(endpoint.hasDefined("name"));
            Assert.assertTrue(endpoint.hasDefined("wsdl-url"));
            Assert.assertTrue(endpoint.get("wsdl-url").asString().endsWith("?wsdl"));
        }
    }

    @Test
    public void testManagementDescriptionMetrics() throws Exception {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.DEPLOYMENT, "ws-example.war");
        address.add(ModelDescriptionConstants.SUBSYSTEM, WSExtension.SUBSYSTEM_NAME); //EndpointService
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        final ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            // Get the wsdl again to be sure the endpoint has been hit at least once
            final URL url = new URL(endpoint.get("wsdl-url").asString());
            HttpRequest.get(url.toExternalForm(), 30, TimeUnit.SECONDS);

            // Read metrics
            checkCountMetric(endpointResult, managementClient.getControllerClient(), "request-count");
            checkCountMetric(endpointResult, managementClient.getControllerClient(), "response-count");
        }
    }

    private void checkCountMetric(final ModelNode endpointResult, final ModelControllerClient client, final String metricName) throws IOException {
    	final ModelNode readAttribute = new ModelNode();
        readAttribute.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readAttribute.get(ModelDescriptionConstants.OP_ADDR).set(endpointResult.get(ModelDescriptionConstants.OP_ADDR));
        readAttribute.get(ModelDescriptionConstants.NAME).set(metricName);

        final ModelNode attribute = client.execute(readAttribute);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, attribute.get(ModelDescriptionConstants.OUTCOME).asString());
        Assert.assertTrue(attribute.get("result").asString().length() == 1);
        Assert.assertTrue(attribute.get("result").asInt() > 0);
    }

    @Test
    public void testAccess() throws Exception {
        URL wsdlURL = new URL(this.url.toExternalForm() + "ws-example?wsdl");
        QName serviceName = new QName("http://webservices.smoke.test.as.jboss.org/", "EndpointService");
        Service service = Service.create(wsdlURL, serviceName);
        Endpoint port = (Endpoint) service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    private String performCall(String params) throws Exception {
        URL url = new URL(this.url.toExternalForm() + "ws-example/" + params);
        return HttpRequest.get(url.toExternalForm(), 30, TimeUnit.SECONDS);
    }
}
