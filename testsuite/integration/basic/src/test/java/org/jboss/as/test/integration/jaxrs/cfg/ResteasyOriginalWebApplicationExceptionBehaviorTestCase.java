/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("auth"))
                            .request()
                            .get()
            ) {
                Assertions.assertEquals(401, response.getStatus());
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(401, response.getStatus(), String.format("Expected 401 but got %d: %s - %s", response.getStatus(), response.getStatusInfo()
                        .getReasonPhrase(), body));
                Assertions.assertTrue(body == null || !body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is NOT expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));
                final MultivaluedMap<String, String> headers = response.getStringHeaders();
                Assertions.assertFalse(headers.containsKey("user-id"), String.format("Expected the response headers %s to not include the user-id header", headers));
            }
        }
    }

    @Test
    @Order(2)
    public void checkDefaultRuntimeException() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("exception"))
                            .request()
                            .get()
            ) {
                Assertions.assertEquals(500, response.getStatus());
                final String body = response.readEntity(String.class);
                Assertions.assertTrue(body == null || !body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is NOT expected in the response: \"%s\"", ErrorResource.TEXT_TO_BE_SANITIZED, body));
            }
        }
    }

    @Test
    @Order(3)
    public void checkDefaultException() {
        try (Client client = ClientBuilder.newClient()) {
            try {
                final String body = client.target(uriBuilder().path("auth"))
                        .request()
                        .get(String.class);
                Assertions.fail("Expected WebApplicationException to be thrown, but got a body of: " + body);
            } catch (WebApplicationException e) {
                final Response response = e.getResponse();
                final String body = response.readEntity(String.class);
                Assertions.assertEquals(401, response.getStatus(), String.format("Expected 401 but got %d: %s - %s", response.getStatus(), response.getStatusInfo()
                        .getReasonPhrase(), body));
                Assertions.assertTrue(body == null || !body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is NOT expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));
                final MultivaluedMap<String, String> headers = response.getStringHeaders();
                Assertions.assertFalse(headers.containsKey("user-id"), String.format("Expected the response headers %s to not include the user-id header", headers));
            }
        }
    }

    @Test
    @Order(4)
    public void checkDefaultExceptionRuntimeException() {
        try (Client client = ClientBuilder.newClient()) {
            try {
                final String body = client.target(uriBuilder().path("exception"))
                        .request()
                        .get(String.class);
                Assertions.fail("Expected WebApplicationException to be thrown, but got a body of: " + body);
            } catch (WebApplicationException e) {
                final Response response = e.getResponse();
                Assertions.assertEquals(500, response.getStatus());
                final String body = response.readEntity(String.class);
                Assertions.assertTrue(body == null || !body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is NOT expected in the response: \"%s\"", ErrorResource.TEXT_TO_BE_SANITIZED, body));
            }
        }
    }

    @Test
    @Order(5)
    public void checkOriginalBehavior() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("auth"))
                            .request()
                            .get()
            ) {
                Assertions.assertEquals(401, response.getStatus());
                final JsonObject body = response.readEntity(JsonObject.class);
                Assertions.assertNotNull(body);
                Assertions.assertTrue(body.containsKey("error"), "Expected an error body in " + body);
                Assertions.assertTrue(body.getString("error")
                        .contains(ErrorResource.TEXT_TO_BE_SANITIZED), String.format("\"%s\" is expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));

                final MultivaluedMap<String, String> headers = response.getStringHeaders();
                Assertions.assertTrue(headers.containsKey("user-id"), String.format("Expected the response headers %s to include the user-id header", headers));
            }
        }
    }

    @Test
    @Order(6)
    public void checkOriginalBehaviorRuntimeException() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder().path("exception"))
                            .request()
                            .get()
            ) {
                Assertions.assertEquals(500, response.getStatus());
                final String body = response.readEntity(String.class);
                Assertions.assertTrue(body != null && body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));
            }
        }
    }

    @Test
    @Order(7)
    public void checkOriginalBehaviorException() {
        try (Client client = ClientBuilder.newClient()) {
            try {
                final String body = client.target(uriBuilder().path("auth"))
                        .request()
                        .get(String.class);
                Assertions.fail("Expected WebApplicationException to be thrown, but got a body of: " + body);
            } catch (WebApplicationException e) {
                final Response response = e.getResponse();
                Assertions.assertEquals(401, response.getStatus());
                final JsonObject body = response.readEntity(JsonObject.class);
                Assertions.assertNotNull(body);
                Assertions.assertTrue(body.containsKey("error"), "Expected an error body in " + body);
                Assertions.assertTrue(body.getString("error")
                        .contains(ErrorResource.TEXT_TO_BE_SANITIZED), String.format("\"%s\" is expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));
                final MultivaluedMap<String, String> headers = response.getStringHeaders();
                Assertions.assertTrue(headers.containsKey("user-id"), String.format("Expected the response headers %s to include the user-id header", headers));
            }
        }
    }

    @Test
    @Order(8)
    public void checkOriginalBehaviorExceptionRuntimeException() {
        try (Client client = ClientBuilder.newClient()) {
            try {
                final String body = client.target(uriBuilder().path("exception"))
                        .request()
                        .get(String.class);
                Assertions.fail("Expected WebApplicationException to be thrown, but got a body of: " + body);
            } catch (WebApplicationException e) {
                final Response response = e.getResponse();
                Assertions.assertEquals(500, response.getStatus());
                final String body = response.readEntity(String.class);
                Assertions.assertTrue(body != null && body.contains(ErrorResource.TEXT_TO_BE_SANITIZED),
                        String.format("\"%s\" is expected in the response!", ErrorResource.TEXT_TO_BE_SANITIZED));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/error/client/");
    }
}
