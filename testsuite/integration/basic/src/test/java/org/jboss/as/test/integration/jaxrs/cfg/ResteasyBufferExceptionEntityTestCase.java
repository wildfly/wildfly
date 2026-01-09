/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ErrorResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-buffer-exception-entity} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ExtendedSnapshotServerSetupTask.class)
public class ResteasyBufferExceptionEntityTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyBufferExceptionEntityTestCase() {
        super("resteasy-buffer-exception-entity");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyBufferExceptionEntityTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, ErrorResource.class);
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.text("checkDefault"))) {
                Assert.assertEquals(500, response.getStatus());
                // Note, we use the RESTEasy internal ClientInvocation here so we can test that the entity was buffered.
                Assert.assertThrows(WebApplicationException.class, () -> ClientInvocation.extractResult(new GenericType<>(String.class), response, null));
                Assert.assertEquals("checkDefault", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkFalse() throws Exception {
        writeAttribute(ModelNode.FALSE);
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.text("checkFalse"))) {
                Assert.assertEquals(500, response.getStatus());
                // Note, we use the RESTEasy internal ClientInvocation here so we can test that the entity was buffered.
                Assert.assertThrows(WebApplicationException.class, () -> ClientInvocation.extractResult(new GenericType<>(String.class), response, null));
                Assert.assertThrows(ProcessingException.class, () -> response.readEntity(String.class));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/error");
    }
}
