/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jackson;

import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.as.test.shared.SecurityManagerFailure;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the resteasy multipart provider
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsJacksonProviderTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addPackage(JaxrsJacksonProviderTestCase.class.getPackage());
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/myjaxrs/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @BeforeClass
    public static void beforeClass() {
        SecurityManagerFailure.thisTestIsFailingUnderSM("https://github.com/FasterXML/jackson-databind/pull/1585");
    }

    private String performCall(String urlPattern) throws Exception {
        return performCall(urlPattern, MediaType.APPLICATION_JSON);
    }

    private String performCall(String urlPattern, final String acceptType) throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(url + urlPattern).request().accept(acceptType).get()) {
                return response.readEntity(String.class);
            }
        }
    }

    @Test
    public void testSimpleJacksonResource() throws Exception {
        String result = performCall("myjaxrs/jackson", "application/vnd.customer+json");
        Assert.assertEquals("{\"first\":\"John\",\"last\":\"Citizen\"}", result);
    }

    @Test
    public void testDurationJsr310() throws Exception {
        String result = performCall("myjaxrs/jackson/duration");
        Assert.assertEquals("\"PT1S\"", result);
    }

    @Test
    public void testDurationJsrjdk8() throws Exception {
        String result = performCall("myjaxrs/jackson/optional");
        Assert.assertEquals("\"optional string\"", result);
    }
    /**
     * AS7-1276
     */
    @Test
    public void testJacksonWithJsonIgnore() throws Exception {
        String result = performCall("myjaxrs/country", "application/vnd.customer+json");
        Assert.assertEquals("{\"name\":\"Australia\",\"temperature\":\"Hot\"}", result);
    }

    /**
     * WFLY-20898: Tests that when both XML and Jackson JSON annotations are used on an entity, that the correct
     * name is used in the serialized object.
     */
    @Test
    public void jsonAnnotationBinding() throws Exception {
        final String result = performCall("myjaxrs/jackson/named", MediaType.APPLICATION_JSON);
        Assert.assertEquals("{\"jsonId\":1,\"jsonName\":\"Jackson\"}", result);
    }

    /**
     * WFLY-20898: Tests that when both XML and Jackson JSON annotations are used on an entity, that the correct
     * name is used in the serialized object.
     */
    @Test
    public void xmlAnnotationBinding() throws Exception {
        final String result = performCall("myjaxrs/jackson/named", MediaType.APPLICATION_XML);
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><named-entity><xml-id>1</xml-id><xml-name>Jackson</xml-name></named-entity>";
        Assert.assertEquals(expectedXml, result);
    }

}
