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
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jsonp.JsonObjectProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-use-builtin-providers} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ResteasyUseBuiltInProvidersTestCase.WriteAttributeServerSetupTask.class)
public class ResteasyUseBuiltInProvidersTestCase extends AbstractResteasyAttributeTest {

    // Register the JsonObjectProvider that we will need to write the response in the ProviderCheckResource
    static class WriteAttributeServerSetupTask extends AbstractWriteAttributesServerSetupTask {

        WriteAttributeServerSetupTask() {
            super(Map.of("resteasy-providers", providers()));
        }

        private static ModelNode providers() {
            final ModelNode value = new ModelNode().setEmptyList();
            value.add(JsonObjectProvider.class.getName());
            return value;
        }
    }

    @ArquillianResource
    private URI uri;

    public ResteasyUseBuiltInProvidersTestCase() {
        super("resteasy-use-builtin-providers");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyUseBuiltInProvidersTestCase.class.getSimpleName() + ".war")
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
    @InSequence(11)
    public void checkFalseBuiltInReader() throws Exception {
        writeAttribute(ModelNode.FALSE);
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
                Assert.assertTrue(String.format("Should not have found built-in provider for %s", String.class), json.isNull("type"));
            }
        }
    }

    @Test
    @InSequence(12)
    public void checkFalseBuiltInWriter() throws Exception {
        writeAttribute(ModelNode.FALSE);
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
                Assert.assertTrue(String.format("Should not have found built-in provider for %s", String.class), json.isNull("type"));
            }
        }
    }

    @Test
    @InSequence(13)
    public void checkFalseUserDefinedReader() throws Exception {
        writeAttribute(ModelNode.FALSE);
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
    @InSequence(14)
    public void checkFalseUserDefinedWriter() throws Exception {
        writeAttribute(ModelNode.FALSE);
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

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/");
    }
}
