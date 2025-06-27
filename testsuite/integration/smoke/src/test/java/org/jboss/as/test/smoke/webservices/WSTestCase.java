/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.webservices;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @version $Revision: 1.1 $
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WSTestCase {

    private static final ModelNode webserviceAddress;

    static {
        webserviceAddress = new ModelNode();
        webserviceAddress.add("subsystem", "webservices");
    }

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
    @Order(1)
    public void testWSDL() throws Exception {
        String wsdl = performCall("?wsdl");
        assertNotNull(wsdl);
        assertThat(wsdl, containsString("wsdl:definitions"));
    }

    @Test
    @Order(2)
    public void testManagementDescription() throws Exception {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.DEPLOYMENT, "ws-example.war");
        address.add(ModelDescriptionConstants.SUBSYSTEM, "webservices"); //EndpointService
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        final ModelNode result = managementClient.getControllerClient().execute(operation);
        List<ModelNode> endpoints = DomainTestSupport.validateResponse(result).asList();
        assertThat(endpoints.size() > 0, is(true));
        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            assertThat(endpoint.hasDefined("class"), is(true));
            assertThat(endpoint.hasDefined("name"), is(true));
            assertThat(endpoint.hasDefined("wsdl-url"), is(true));
            assertThat(endpoint.get("wsdl-url").asString().endsWith("?wsdl"), is(true));
            assertThat(endpoint.hasDefined("request-count"), is(true));
            assertThat(endpoint.get("request-count").asString(), is("0"));
        }
    }

    @Test
    @Order(3)
    public void testManagementDescriptionMetrics() throws Exception {
        setStatisticsEnabled(true);
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.DEPLOYMENT, "ws-example.war");
        address.add(ModelDescriptionConstants.SUBSYSTEM, "webservices"); //EndpointService
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        ModelNode result = managementClient.getControllerClient().execute(operation);
        List<ModelNode> endpoints = DomainTestSupport.validateResponse(result).asList();
        assertThat(endpoints.size() > 0, is(true));
        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            // Get the wsdl again to be sure the endpoint has been hit at least once
            final URL wsdlUrl = new URL(endpoint.get("wsdl-url").asString());
            String wsdl = HttpRequest.get(wsdlUrl.toExternalForm(), 30, TimeUnit.SECONDS);
            assertThat(wsdl, is(notNullValue()));

            // Read metrics
            checkCountMetric(endpointResult, managementClient.getControllerClient(), "request-count");
            checkCountMetric(endpointResult, managementClient.getControllerClient(), "response-count");
        }

        setStatisticsEnabled(false);
        result = managementClient.getControllerClient().execute(operation);
        endpoints = DomainTestSupport.validateResponse(result).asList();
        for (final ModelNode endpointResult : endpoints) {
            final ModelNode endpoint = endpointResult.get("result");
            assertThat(endpoint.hasDefined("request-count"), is(true));
            assertThat(endpoint.get("request-count").asString(), is("1"));
        }
    }

    private int checkCountMetric(final ModelNode endpointResult, final ModelControllerClient client, final String metricName) throws IOException {
        final ModelNode readAttribute = new ModelNode();
        readAttribute.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readAttribute.get(ModelDescriptionConstants.OP_ADDR).set(endpointResult.get(ModelDescriptionConstants.OP_ADDR));
        readAttribute.get(ModelDescriptionConstants.NAME).set(metricName);
        long timeout = 30_000L + System.currentTimeMillis();
        String value = "-1";
        while (System.currentTimeMillis() < timeout) {
            ModelNode attribute = client.execute(readAttribute);
            ModelNode result = DomainTestSupport.validateResponse(attribute);
            value = result.asString();
            assertThat("We have found " + result, value.length(), is(1));
            if (result.asInt() > 0) {
                //We have found a valid metric
                return result.asInt();
            }
        }
        fail("We have found " + value + " for metric " + metricName + " instead of some positive value");
        return -1;
    }

    @Test
    @Order(4)
    public void testAccess() throws Exception {
        URL wsdlURL = new URL(this.url.toExternalForm() + "ws-example?wsdl");
        QName serviceName = new QName("http://webservices.smoke.test.as.jboss.org/", "EndpointService");
        Service service = Service.create(wsdlURL, serviceName);
        Endpoint port = (Endpoint) service.getPort(Endpoint.class);
        String echo = port.echo("Foo");
        assertThat("Echoing Foo should return Foo not " + echo, echo, is("Foo"));
    }

    private String performCall(String params) throws Exception {
        URL callUrl = new URL(this.url.toExternalForm() + "ws-example/" + params);
        return HttpRequest.get(callUrl.toExternalForm(), 30, TimeUnit.SECONDS);
    }


    private void setStatisticsEnabled(boolean enabled) throws Exception {
        final ModelNode updateStatistics = new ModelNode();
        updateStatistics.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        updateStatistics.get(ModelDescriptionConstants.OP_ADDR).set(webserviceAddress);
        updateStatistics.get(ModelDescriptionConstants.NAME).set("statistics-enabled");
        updateStatistics.get(ModelDescriptionConstants.VALUE).set(enabled);
        final ModelNode result = managementClient.getControllerClient().execute(updateStatistics);
        DomainTestSupport.validateResponse(result, false);
    }
}
