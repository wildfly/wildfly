/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;
import java.util.Set;

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
import org.jboss.as.arquillian.setup.ConfigureLoggingSetupTask;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
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
 * Tests the {@code resteasy-patchfilter-disabled} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup({ConfigureLoggingSetupTask.class, SnapshotServerSetupTask.class})
public class ResteasyPatchFilterDisabledTestCase extends AbstractResteasyAttributeTest {

    @ArquillianResource
    private URI uri;

    public ResteasyPatchFilterDisabledTestCase() {
        super("resteasy-patchfilter-disabled");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyPatchFilterDisabledTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        UserResource.class,
                        User.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            final long id = 1;
            final User addUser = new User();
            addUser.setId(id);
            addUser.setName("resteasy");
            addUser.setRoles(Set.of(User.Role.ADMIN, User.Role.MANAGER));
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.json(addUser))
            ) {
                Assert.assertEquals(201, response.getStatus());
                final User user = client.target(response.getLocation()).request().get(User.class);
                Assert.assertEquals(String.format("Expected id %d for user %s", id, user.getName()), id, user.getId());
                Assert.assertEquals("resteasy", user.getName());
                Assert.assertTrue(String.format("Expected role ADMIN for %s", user), user.getRoles().contains(User.Role.ADMIN));
                Assert.assertTrue(String.format("Expected role MANAGER for %s", user), user.getRoles().contains(User.Role.MANAGER));
                Assert.assertFalse(String.format("Expected role USER for %s to not be present", user), user.getRoles().contains(User.Role.USER));
            }
            // Attempt to send a patch ot the user
            try (
                    Response response = client.target(uriBuilder().path(Long.toString(id)))
                            .request()
                            .method("PATCH", Entity.entity("{\"roles\": [\"MANAGER\", \"USER\"]}", "application/merge-patch+json"))
            ) {
                Assert.assertEquals(204, response.getStatus());
                final User user = client.target(response.getLocation()).request().get(User.class);
                Assert.assertEquals("resteasy", user.getName());
                Assert.assertTrue(String.format("Expected role MANAGER for %s", user), user.getRoles().contains(User.Role.MANAGER));
                Assert.assertTrue(String.format("Expected role USER for %s", user), user.getRoles().contains(User.Role.USER));
                Assert.assertFalse(String.format("Expected role ADMIN for %s ot not be present", user), user.getRoles().contains(User.Role.ADMIN));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkPatchDisabled() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (Client client = ClientBuilder.newClient()) {
            final long id = 2;
            final User addUser = new User();
            addUser.setId(id);
            addUser.setName("resteasy");
            addUser.setRoles(Set.of(User.Role.ADMIN, User.Role.MANAGER));
            try (
                    Response response = client.target(uriBuilder())
                            .request()
                            .post(Entity.json(addUser))
            ) {
                Assert.assertEquals(201, response.getStatus());
                final User user = client.target(response.getLocation()).request().get(User.class);
                Assert.assertEquals(String.format("Expected id %d for user %s", id, user.getName()), id, user.getId());
                Assert.assertEquals("resteasy", user.getName());
                Assert.assertTrue(String.format("Expected role ADMIN for %s", user), user.getRoles().contains(User.Role.ADMIN));
                Assert.assertTrue(String.format("Expected role MANAGER for %s", user), user.getRoles().contains(User.Role.MANAGER));
                Assert.assertFalse(String.format("Expected role USER for %s to not be present", user), user.getRoles().contains(User.Role.USER));
            }
            // Attempt to send a patch ot the user
            try (
                    Response response = client.target(uriBuilder().path(Long.toString(id)))
                            .request()
                            .method("PATCH", Entity.entity("{\"roles\": [\"MANAGER\", \"USER\"]}", "application/merge-patch+json"))
            ) {
                // Patching should be disabled resulting in a 400 and null fields
                Assert.assertEquals(400, response.getStatus());
                final User user = response.readEntity(User.class);
                Assert.assertEquals(String.format("User.getId() expected to be -1 for %s", user), -1L, user.getId());
                Assert.assertNull(String.format("User.getName() expected to be null for %s", user), user.getName());
                Assert.assertTrue(String.format("Expected role MANAGER for %s", user), user.getRoles().contains(User.Role.MANAGER));
                Assert.assertTrue(String.format("Expected role USER for %s", user), user.getRoles().contains(User.Role.USER));
                Assert.assertFalse(String.format("Expected role ADMIN for %s ot not be present", user), user.getRoles().contains(User.Role.ADMIN));
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/user/");
    }
}
