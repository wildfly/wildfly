/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;
import java.util.List;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletRequest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.EchoResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ParameterFilter;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SimpleText;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-use-container-form-params} attribute works as expected.
 * <p>
 * When a {@link jakarta.servlet.Filter} accesses the {@link ServletRequest#getParameterMap()} or the
 * {@link ServletRequest#getInputStream()} is read, this setting is required to retrieve the form parameters form the
 * request.
 * </p>
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyUseContainerFormParamsTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyUseContainerFormParamsTestCase() {
        super("resteasy-use-container-form-params");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyUseContainerFormParamsTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        EchoResource.class,
                        SimpleText.class,
                        ParameterFilter.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkFormDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("/form"))
                            .request()
                            .post(Entity.form(new Form("param1", "value1").param("param2", "value2")))
            ) {
                Assert.assertEquals(400, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);
                Assert.assertEquals(String.format("Expected the JSON no form data to be found in %s", json),
                        "no form data", json.getString("error"));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkMapDefault() {
        try (Client client = ClientBuilder.newClient()) {
            final MultivaluedMap<String, String> map = new MultivaluedHashMap<>(2);
            map.put("param1", List.of("value1"));
            map.put("param2", List.of("value2"));
            try (
                    Response response = client.target(uriBuilder().path("/form/map"))
                            .request()
                            .post(Entity.form(map))
            ) {
                Assert.assertEquals(400, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);
                Assert.assertEquals(String.format("Expected the JSON no form data to be found in %s", json),
                        "no form data", json.getString("error"));
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkFormTrue() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("/form"))
                            .request()
                            .post(Entity.form(new Form("param1", "value1").param("param2", "value2")))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);

                final JsonArray array1 = json.getJsonArray("param1");
                Assert.assertNotNull(String.format("Did not find param1 in %s", json), array1);
                Assert.assertEquals(String.format("No data found for param1 in %s", json), "value1", array1.getString(0));

                final JsonArray array2 = json.getJsonArray("param2");
                Assert.assertNotNull(String.format("Did not find param2 in %s", json), array2);
                Assert.assertEquals(String.format("No data found for param2 in %s", json), "value2", array2.getString(0));
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkMapTrue() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            final MultivaluedMap<String, String> map = new MultivaluedHashMap<>(2);
            map.put("param1", List.of("value1"));
            map.put("param2", List.of("value2"));
            try (
                    Response response = client.target(uriBuilder().path("/form/map"))
                            .request()
                            .post(Entity.form(map))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertNotNull(json);

                final JsonArray array1 = json.getJsonArray("param1");
                Assert.assertNotNull(String.format("Did not find param1 in %s", json), array1);
                Assert.assertEquals(String.format("No data found for param1 in %s", json), "value1", array1.getString(0));

                final JsonArray array2 = json.getJsonArray("param2");
                Assert.assertNotNull(String.format("Did not find param2 in %s", json), array2);
                Assert.assertEquals(String.format("No data found for param2 in %s", json), "value2", array2.getString(0));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/echo/");
    }

}
