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
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.test.integration.jaxrs.cfg.resources.ObjectMapperContextResolver;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.integration.jaxrs.cfg.resources.User;
import org.jboss.as.test.integration.jaxrs.cfg.resources.UserResource;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-prefer-jackson-over-jsonb} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(SnapshotServerSetupTask.class)
public class ResteasyPreferJacksonOverJsonbTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyPreferJacksonOverJsonbTestCase() {
        super("resteasy-prefer-jackson-over-jsonb");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyPreferJacksonOverJsonbTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        UserResource.class,
                        User.class,
                        ObjectMapperContextResolver.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.json("{\"name\":\"resteasy\",\"roles\": [\"ADMIN\", \"MANAGER\"]}"))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final User user = response.readEntity(User.class);
                Assert.assertEquals("resteasy", user.getName());
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkJackson() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.json("{\"name\":\"resteasy\",\"roles\": [\"admin\", \"user\"]}"))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final User user = response.readEntity(User.class);
                Assert.assertEquals("resteasy", user.getName());
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkJacksonInvalidProperty() throws Exception {
        writeAttribute(ModelNode.FALSE);
        try (Client client = ClientBuilder.newClient()) {
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.json("{\"name\":\"resteasy\",\"roles\": [\"MANAGER\"],\"invalid\": true}"))
            ) {
                Assert.assertEquals(200, response.getStatus());
                final User user = response.readEntity(User.class);
                Assert.assertEquals("resteasy", user.getName());
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/user/echo");
    }
}
