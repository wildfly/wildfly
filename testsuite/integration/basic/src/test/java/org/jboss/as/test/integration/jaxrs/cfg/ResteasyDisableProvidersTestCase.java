/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.EchoResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ProviderCheckResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleText;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleTextReaderWriter;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-disable-providers} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyDisableProvidersTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyDisableProvidersTestCase() {
        super("resteasy-disable-providers");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyDisableProvidersTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class,
                        EchoResource.class,
                        ProviderCheckResource.class,
                        SimpleText.class,
                        SimpleTextReaderWriter.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefaultBuiltInReader() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("provider-check/reader")
                                    .queryParam("type", String.class.getName())
                                    .queryParam("genericType", String.class.getName()))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                final String className = StringTextStar.class.getName();
                Assert.assertFalse(String.format("Failed to find provider for %s", String.class), json.isNull("type"));
                Assert.assertTrue(String.format("Expected %s to be the base class name in %s", className, json), json.getString("type")
                        .startsWith(className));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkDefaultBuiltInWriter() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("provider-check/writer")
                                    .queryParam("type", String.class.getName())
                                    .queryParam("genericType", String.class.getName()))
                            .request(MediaType.APPLICATION_JSON)
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                final String className = StringTextStar.class.getName();
                Assert.assertFalse(String.format("Failed to find provider for %s", String.class), json.isNull("type"));
                Assert.assertTrue(String.format("Expected %s to be the base class name in %s", className, json), json.getString("type")
                        .startsWith(className));
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkDefaultUserDefinedReader() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("provider-check/reader")
                                    .queryParam("type", SimpleText.class.getName())
                                    .queryParam("genericType", SimpleText.class.getName())
                                    .queryParam("mediaType", MediaType.TEXT_PLAIN))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                final String className = SimpleTextReaderWriter.class.getName();
                Assert.assertFalse(String.format("Failed to find provider for %s", String.class), json.isNull("type"));
                Assert.assertTrue(String.format("Expected %s to be the base class name in %s", className, json), json.getString("type")
                        .startsWith(className));
            }
        }
    }

    @Test
    @InSequence(4)
    public void checkDefaultUserDefinedWriter() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("provider-check/writer")
                                    .queryParam("type", SimpleText.class.getName())
                                    .queryParam("genericType", SimpleText.class.getName())
                                    .queryParam("mediaType", MediaType.TEXT_PLAIN))
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                final String className = SimpleTextReaderWriter.class.getName();
                Assert.assertFalse(String.format("Failed to find provider for %s", String.class), json.isNull("type"));
                Assert.assertTrue(String.format("Expected %s to be the base class name in %s", className, json), json.getString("type")
                        .startsWith(className));
            }
        }
    }

    @Test
    @InSequence(5)
    public void checkDefaultBuiltIn() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("echo/text"))
                            .request()
                            .post(Entity.text("stringStarEnabled"))
            ) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("stringStarEnabled", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(6)
    public void checkDefaultUserDefined() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("echo/simple-text"))
                            .request()
                            .post(Entity.text(new SimpleText("simple-text")))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final SimpleText simpleText = response.readEntity(SimpleText.class);
                Assert.assertEquals("simple-text", simpleText.getText());
            }
        }
    }

    @Test
    @InSequence(11)
    public void checkDisableBuiltInProvider() throws Exception {
        final ModelNode disabledProviders = new ModelNode().setEmptyList();
        disabledProviders.add(StringTextStar.class.getName());
        writeAttribute(disabledProviders);

        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("echo/text"))
                            .request()
                            .post(Entity.text("stringStarDisabled"))
            ) {
                Assert.assertEquals(415, response.getStatus());
            }
        }
    }

    @Test
    @InSequence(12)
    public void checkDisableUserDefinedProvider() throws Exception {
        final ModelNode disabledProviders = new ModelNode().setEmptyList();
        disabledProviders.add(SimpleTextReaderWriter.class.getName());
        writeAttribute(disabledProviders);

        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("echo/simple-text"))
                            .request()
                            .post(Entity.text(new SimpleText("simple-text-error")))
            ) {
                Assert.assertEquals(200, response.getStatus());
                // We can use the SimpleTextReaderWriter because we're on the client site
                final SimpleText simpleText = response.readEntity(SimpleText.class);
                Assert.assertEquals("simple-text-error", simpleText.getText());
            }
        }
    }

    @Test
    @InSequence(13)
    public void checkDisableProvider() throws Exception {
        // Disable all providers which can write text/plain
        final ModelNode disabledProviders = new ModelNode().setEmptyList();
        disabledProviders.add(StringTextStar.class.getName());
        disabledProviders.add(SimpleTextReaderWriter.class.getName());
        disabledProviders.add(DefaultTextPlain.class.getName());
        writeAttribute(disabledProviders);

        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("echo/simple-text"))
                            .request()
                            .post(Entity.text(new SimpleText("simple-text-error")))
            ) {
                Assert.assertEquals(415, response.getStatus());
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/");
    }
}
