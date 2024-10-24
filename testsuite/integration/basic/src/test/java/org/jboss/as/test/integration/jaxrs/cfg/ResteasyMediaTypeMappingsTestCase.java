/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.EchoResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleText;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the {@code resteasy-media-type-mappings} attribute works as expected.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ResteasyMediaTypeMappingsTestCase.WriteAttributeServerSetupTask.class)
@RunAsClient
public class ResteasyMediaTypeMappingsTestCase {

    static class WriteAttributeServerSetupTask extends AbstractWriteAttributesServerSetupTask {
        WriteAttributeServerSetupTask() {
            super(Map.of("resteasy-media-type-mappings", createValue()));
        }

        private static ModelNode createValue() {
            final ModelNode value = new ModelNode().setEmptyObject();
            value.get("json").set("application/json");
            value.get("txt").set("text/plain");
            return value;
        }
    }

    @ArquillianResource
    private URI uri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyMediaTypeMappingsTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        EchoResource.class,
                        SimpleText.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void checkJson() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder("json"))
                            .request()
                            .post(Entity.json(new SimpleText("jsonTest")))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);
                Assert.assertEquals("jsonTest", json.getString("text"));
            }
        }
    }

    @Test
    public void checkText() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder("txt"))
                            .request()
                            .post(Entity.text(new SimpleText("plainText")))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final SimpleText text = response.readEntity(SimpleText.class);
                Assert.assertEquals("plainText", text.getText());
            }
        }
    }

    private UriBuilder uriBuilder(final String ext) {
        return UriBuilder.fromUri(uri).path(String.format("/test/echo/simple-text.%s", ext));
    }
}
