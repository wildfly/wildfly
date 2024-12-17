/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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
 * Tests the {@code resteasy-language-mappings} attribute works as expected.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ResteasyLanguageMappingsTestCase.WriteAttributeServerSetupTask.class)
@RunAsClient
public class ResteasyLanguageMappingsTestCase {

    static class WriteAttributeServerSetupTask extends AbstractWriteAttributesServerSetupTask {
        WriteAttributeServerSetupTask() {
            super(Map.of("resteasy-language-mappings", createValue()));
        }

        private static ModelNode createValue() {
            final ModelNode value = new ModelNode().setEmptyObject();
            value.get("en").set("en-US");
            value.get("fr").set("fr-FR");
            return value;
        }
    }

    @ArquillianResource
    private URI uri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyLanguageMappingsTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        EchoResource.class,
                        SimpleText.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void checkEn() {
        assertHeader("en", "US");
    }

    @Test
    public void checkFr() {
        assertHeader("fr", "FR");
    }

    private void assertHeader(final String lang, final String country) {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder(lang))
                            .request()
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);
                Assert.assertTrue(String.format("Expected acceptedLanguages to be set in %s", json), json.containsKey("acceptedLanguages"));
                final JsonArray acceptedLanguages = json.getJsonArray("acceptedLanguages");
                Assert.assertEquals(1, acceptedLanguages.size());
                Assert.assertEquals(String.format("%s-%s", lang, country), acceptedLanguages.getString(0));
            }
        }
    }

    private UriBuilder uriBuilder(final String lang) {
        return UriBuilder.fromUri(uri).path(String.format("/test/echo/headers.%s", lang));
    }
}
