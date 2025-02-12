/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient;

import java.net.URI;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HeaderPropagationTestCase {
    private static final String CONFIG_PROPERTIES = "org.eclipse.microprofile.rest.client.propagateHeaders=TestPropagated\n" +
            "org.wildfly.test.integration.microprofile.restclient.TestClient/mp-rest/uri=http://" +
            TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/" + HeaderPropagationTestCase.class.getSimpleName() + "\n";

    @ArquillianResource
    private URI uri;

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, HeaderPropagationTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        Headers.class,
                        ClientResource.class,
                        ServerResource.class,
                        TestClient.class,
                        TestApplication.class
                )
                .addAsManifestResource(new StringAsset(CONFIG_PROPERTIES), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Tests that the TestPropagated header is in the HTTP request from the REST client and that it is propagated to the
     * {@linkplain TestClient MP REST client}. The MP REST client should also contain a header defined in the
     * {@link TestClient} itself.
     */
    @Test
    public void checkTestPropagatedHeader() {
        try (Client client = ClientBuilder.newClient()) {
            final JsonObject json = client.target(UriBuilder.fromUri(uri).path("api/client"))
                    .request()
                    .header("TestPropagated", "test-value")
                    .get(JsonObject.class);
            // We should have the TestPropagated header listed in the config properties
            Assert.assertEquals("TestPropagated", json.getString("org.eclipse.microprofile.rest.client.propagateHeaders"));
            // The incoming headers should include the TestPropagated header with the value "test-value"
            Assert.assertEquals("test-value", getHeaderValue(json.getJsonObject("incomingRequestHeaders"), "TestPropagated"));
            // The serverResponse should also have this header, plus the TestClientHeader
            final JsonObject serverHeaders = json.getJsonObject("serverResponse");
            Assert.assertEquals("test-value", getHeaderValue(serverHeaders, "TestPropagated"));
            Assert.assertEquals("client-value", getHeaderValue(serverHeaders, "TestClientHeader"));
        }
    }

    private String getHeaderValue(final JsonObject json, final String headerName) {
        final JsonArray array = json.getJsonArray(headerName);
        Assert.assertNotNull(String.format("Header %s was not found in %s", headerName, json), array);
        Assert.assertFalse(String.format("Header %s has no values: %s", headerName, json), array.isEmpty());
        return array.getString(0);
    }
}
