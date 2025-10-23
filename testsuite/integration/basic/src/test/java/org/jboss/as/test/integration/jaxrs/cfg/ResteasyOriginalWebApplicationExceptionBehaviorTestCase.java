/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.setup.ConfigureLoggingSetupTask;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ErrorResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-original-webapplicationexception-behavior} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup({ConfigureLoggingSetupTask.class, SnapshotServerSetupTask.class})
public class ResteasyOriginalWebApplicationExceptionBehaviorTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyOriginalWebApplicationExceptionBehaviorTestCase() {
        super("resteasy-original-webapplicationexception-behavior");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyOriginalWebApplicationExceptionBehaviorTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        ErrorResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertTrue(String.format("Expected the response %s to include headers", json), json.containsKey("headers"));
                final JsonObject headers = json.getJsonObject("headers");
                Assert.assertFalse(String.format("Expected the response %s to not include the user-id header", headers), headers.containsKey("user-id"));
                Assert.assertFalse(String.format("Expected the response %s to not include an error body", json), json.containsKey("error"));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkOriginalBehavior() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .get()
            ) {
                Assert.assertEquals(200, response.getStatus());
                final JsonObject json = response.readEntity(JsonObject.class);
                Assert.assertTrue(String.format("Expected the response %s to include headers", json), json.containsKey("headers"));
                final JsonObject headers = json.getJsonObject("headers");
                Assert.assertTrue(String.format("Expected the response %s to include the user-id header", headers), headers.containsKey("user-id"));
                Assert.assertTrue(String.format("Expected the response %s to include an error body", json), json.containsKey("error"));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/error/client/no-auth");
    }
}
