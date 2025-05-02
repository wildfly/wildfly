/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import dev.resteasy.client.util.authentication.HttpAuthenticators;
import dev.resteasy.client.util.authentication.UserCredentials;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.jaxrs.cfg.resources.SecureResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.testing.tools.deployments.DeploymentDescriptors;

/**
 * Tests the {@code resteasy-role-based-security} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ResteasyRoleBasedSecurityTestCase.SecurityDomainServerSetupTask.class)
public class ResteasyRoleBasedSecurityTestCase extends AbstractResteasyAttributeTest {

    static class SecurityDomainServerSetupTask extends ExtendedSnapshotServerSetupTask {
        private static final Logger LOGGER = Logger.getLogger(SecurityDomainServerSetupTask.class);
        // Properties file path
        private static final String USERS_FILENAME = "test-users.properties";
        private static final String ROLES_FILENAME = "test-roles.properties";

        private final Set<Path> filesToRemove = new HashSet<>();

        @Override
        protected void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode address = Operations.createAddress("path", "jboss.server.config.dir");
            ModelNode op = Operations.createReadAttributeOperation(address, "path");
            final Path configDir = Path.of(executeOperation(managementClient, op).asString());

            final Path usersFile = configDir.resolve(USERS_FILENAME);
            final Path rolesFile = configDir.resolve(ROLES_FILENAME);
            final Properties users = new Properties();
            final Properties roles = new Properties();
            users.setProperty("admin", "admin.12345!");
            users.setProperty("manager", "manager.12345!");
            users.setProperty("user", "user.12345!");

            roles.setProperty("admin", "admin");
            roles.setProperty("manager", "manager");
            roles.setProperty("user", "user");

            try (
                    BufferedWriter userWriter = Files.newBufferedWriter(usersFile, StandardCharsets.UTF_8);
                    BufferedWriter roleWriter = Files.newBufferedWriter(rolesFile, StandardCharsets.UTF_8)
            ) {
                users.store(userWriter, null);
                roles.store(roleWriter, null);
            }

            filesToRemove.add(usersFile);
            filesToRemove.add(rolesFile);

            // Create the operation builder
            final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();

            // Create Elytron properties-realm
            address = Operations.createAddress("subsystem", "elytron", "properties-realm", REALM_NAME);
            op = Operations.createAddOperation(address);
            final ModelNode userProperties = op.get("users-properties").setEmptyObject();
            userProperties.get("path").set(USERS_FILENAME);
            userProperties.get("relative-to").set("jboss.server.config.dir");
            userProperties.get("plain-text").set(true);

            final ModelNode groupProperties = op.get("groups-properties").setEmptyObject();
            groupProperties.get("path").set(ROLES_FILENAME);
            groupProperties.get("relative-to").set("jboss.server.config.dir");
            builder.addStep(op);

            // Create Elytron security-domain
            address = Operations.createAddress("subsystem", "elytron", "security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(address);
            final ModelNode realms = new ModelNode().setEmptyObject();
            realms.get("realm").set(REALM_NAME);
            realms.get("role-decoder").set("groups-to-roles");
            op.get("realms").setEmptyList().add(realms);

            op.get("default-realm").set(REALM_NAME);
            op.get("permission-mapper").set("default-permission-mapper");
            builder.addStep(op);

            // Create Elytron http-authentication-factory with previous security-domain
            address = Operations.createAddress("subsystem", "elytron", "http-authentication-factory",
                    "http-auth-" + DOMAIN_NAME);
            op = Operations.createAddOperation(address);

            // Create the value for the mechanism-configurations
            final ModelNode mechanismConfigs = new ModelNode().setEmptyObject();
            mechanismConfigs.get("mechanism-name").set("BASIC");
            final ModelNode mechanisms = mechanismConfigs.get("mechanism-realm-configurations").setEmptyList();
            final ModelNode mechanismsValue = new ModelNode().setEmptyObject();
            mechanismsValue.get("realm-name").set("propRealm");
            mechanisms.add(mechanismsValue);

            op.get("mechanism-configurations").setEmptyList().add(mechanismConfigs);
            op.get("http-server-mechanism-factory").set("global");
            op.get("security-domain").set(DOMAIN_NAME);
            builder.addStep(op);

            // Set undertow application-security-domain to the custom http-authentication-factory
            address = Operations.createAddress("subsystem", "undertow", "application-security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(address);
            op.get("http-authentication-factory").set("http-auth-" + DOMAIN_NAME);
            builder.addStep(op);
            executeOperation(managementClient, builder.build());
        }

        @Override
        protected void nonManagementCleanUp() {
            for (Path file : filesToRemove) {
                try {
                    if (Files.notExists(file))
                        Files.delete(file);
                } catch (IOException e) {
                    LOGGER.warnf(e, "Failed to delete file %s", file);
                }
            }
        }
    }

    private static final String DOMAIN_NAME = "test-rest-security-domain";
    private static final String REALM_NAME = "test-rest-realm";

    @ArquillianResource
    private URI uri;

    public ResteasyRoleBasedSecurityTestCase() {
        super("resteasy-role-based-security");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        final WebAppDescriptor webXml = Descriptors.create(WebAppDescriptor.class);
        webXml.createLoginConfig()
                .authMethod("BASIC")
                .realmName("Secured Endpoints");
        webXml.createSecurityRole()
                .roleName("admin");
        webXml.createSecurityRole()
                .roleName("manager");
        webXml.createSecurityRole()
                .roleName("user");

        final var securityConstraint = webXml.createSecurityConstraint();
        securityConstraint.createWebResourceCollection()
                .webResourceName("secured-endpoints")
                .urlPattern("/test/secure/*");
        securityConstraint.getOrCreateAuthConstraint()
                .roleName("admin", "manager", "user");

        return ShrinkWrap.create(WebArchive.class, ResteasyRoleBasedSecurityTestCase.class.getSimpleName() + ".war")
                .addClasses(
                        TestApplication.class,
                        SecureResource.class)
                .addAsWebInfResource(new StringAsset(webXml.exportAsString()), "web.xml")
                .addAsWebInfResource(DeploymentDescriptors.createJBossWebSecurityDomain(DOMAIN_NAME), "jboss-web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @InSequence(1)
    public void checkDefaultAdmin() {
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("admin", "admin.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("admin", response.readEntity(String.class));
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("manager", response.readEntity(String.class));
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkDefaultManager() {
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("manager", "manager.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("admin", response.readEntity(String.class));
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("manager", response.readEntity(String.class));
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkDefaultUser() {
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("user", "user.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("admin", response.readEntity(String.class));
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("manager", response.readEntity(String.class));
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(4)
    public void checkTrueAdmin() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("admin", "admin.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("admin", response.readEntity(String.class));
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("manager", response.readEntity(String.class));
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(5)
    public void checkTrueManager() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("manager", "manager.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(403, response.getStatus());
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("manager", response.readEntity(String.class));
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkTrueUser() throws Exception {
        writeAttribute(ModelNode.TRUE);
        try (
                Client client = ClientBuilder.newBuilder()
                        .register(HttpAuthenticators.basic(UserCredentials.clear("user", "user.12345!".toCharArray())))
                        .build()
        ) {
            try (Response response = getAdmin(client)) {
                Assert.assertEquals(403, response.getStatus());
            }
            try (Response response = getManager(client)) {
                Assert.assertEquals(403, response.getStatus());
            }
            try (Response response = getUser(client)) {
                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals("user", response.readEntity(String.class));
            }
        }
    }

    private Response getAdmin(final Client client) {
        return client.target(uriBuilder().path("admin")).request().get();
    }

    private Response getManager(final Client client) {
        return client.target(uriBuilder().path("manager")).request().get();
    }

    private Response getUser(final Client client) {
        return client.target(uriBuilder().path("user")).request().get();
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/secure");
    }
}
