/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.test.integration.jaxrs.cfg.resources.EchoResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleText;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests the {@code resteasy-add-charset} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(SnapshotServerSetupTask.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResteasyAddCharsetTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyAddCharsetTestCase() {
        super("resteasy-add-charset");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyAddCharsetTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, EchoResource.class, SimpleText.class);
    }

    @Test
    @Order(1)
    public void checkTextDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder().path("/text")).request().post(Entity.text("test"))) {
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(200, response.getStatus(), String.format("Invalid response code %d: %s", response.getStatus(), body));
                Assertions.assertEquals("test", body);
                final String contentType = response.getHeaderString("Content-Type");
                Assertions.assertNotNull(contentType);
                Assertions.assertEquals("text/plain;charset=UTF-8", contentType);
            }
        }
    }

    @Test
    @Order(2)
    public void checkXmlDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("/xml"))
                            .request()
                            .post(Entity.xml("<test>test</test>"))
            ) {
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(200, response.getStatus(), String.format("Invalid response code %d: %s", response.getStatus(), body));
                Assertions.assertEquals("<test>test</test>", body);
                final String contentType = response.getHeaderString("Content-Type");
                Assertions.assertNotNull(contentType);
                Assertions.assertEquals("application/xml;charset=UTF-8", contentType);
            }
        }
    }

    @Test
    @Order(3)
    public void checkText() throws Exception {
        writeAttribute(ModelNode.FALSE);
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder().path("/text")).request().post(Entity.text("test"))) {
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(200, response.getStatus(), String.format("Invalid response code %d: %s", response.getStatus(), body));
                Assertions.assertEquals("test", body);
                final String contentType = response.getHeaderString("Content-Type");
                Assertions.assertNotNull(contentType);
                Assertions.assertEquals("text/plain", contentType);
            }
        }
    }

    @Test
    @Order(4)
    public void checkXml() throws Exception {
        writeAttribute(ModelNode.FALSE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("/xml"))
                            .request()
                            .post(Entity.xml("<test>test</test>"))
            ) {
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(200, response.getStatus(), String.format("Invalid response code %d: %s", response.getStatus(), body));
                Assertions.assertEquals("<test>test</test>", body);
                final String contentType = response.getHeaderString("Content-Type");
                Assertions.assertNotNull(contentType);
                Assertions.assertEquals("application/xml", contentType);
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/echo");
    }
}
