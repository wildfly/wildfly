/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.subsystem.adapter.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.SUBSYSTEM_NAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.elytron.oidc.ElytronOidcExtension;

/**
 * Test case for the keycloak migrate op.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class MigrateOperationTestCase extends AbstractSubsystemTest {

    public static final String ELYTRON_OIDC_SUBSYSTEM_NAME = "elytron-oidc-client";

    public MigrateOperationTestCase() {
        super(SUBSYSTEM_NAME, new KeycloakExtension());
    }

    @Test
    public void testMigrateDefaultKeycloakConfig() throws Exception {
        // default config is empty
        String subsystemXml = readResource("keycloak-subsystem-migration-default-config.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

        ModelNode response = services.executeOperation(migrateOp);

        checkOutcome(response);

        ModelNode warnings = response.get(RESULT, "migration-warnings");
        assertEquals(warnings.toString(), 0, warnings.asList().size());

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME);
        ModelNode realm = newSubsystem.get("realm");
        assertFalse(realm.isDefined());
        ModelNode secureDeployment = newSubsystem.get("secure-deployment");
        assertFalse(secureDeployment.isDefined());
        ModelNode secureServer = newSubsystem.get("secure-server");
        assertFalse(secureServer.isDefined());
    }

    @Test
    public void testMigrateNonEmptyKeycloakConfig() throws Exception {
        String subsystemXml = readResource("keycloak-subsystem-migration-non-empty-config.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

        ModelNode response = services.executeOperation(migrateOp);

        checkOutcome(response);

        ModelNode warnings = response.get(RESULT, "migration-warnings");
        assertEquals(warnings.toString(), 0, warnings.asList().size());

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME);
        ModelNode masterRealm = newSubsystem.get("realm", "master");
        assertTrue(masterRealm.isDefined());
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB", masterRealm.get("realm-public-key").asString());
        assertEquals("http://localhost:8080/auth", masterRealm.get("auth-server-url").asString());
        assertEquals("truststore.jks", masterRealm.get("truststore").asString());
        assertEquals("secret", masterRealm.get("truststore-password").asString());
        assertEquals("EXTERNAL", masterRealm.get("ssl-required").asString());
        assertEquals(443, masterRealm.get("confidential-port").asInt());
        assertFalse(masterRealm.get("allow-any-hostname").asBoolean());
        assertTrue(masterRealm.get("disable-trust-manager").asBoolean());
        assertEquals(20, masterRealm.get("connection-pool-size").asInt());
        assertEquals(2000, masterRealm.get("socket-timeout-millis").asInt());
        assertEquals(5000, masterRealm.get("connection-ttl-millis").asInt());
        assertEquals(3000, masterRealm.get("connection-timeout-millis").asInt());
        assertTrue(masterRealm.get("enable-cors").asBoolean());
        assertEquals("keys.jks", masterRealm.get("client-keystore").asString());
        assertEquals("secret", masterRealm.get("client-keystore-password").asString());
        assertEquals("secret", masterRealm.get("client-key-password").asString());
        assertEquals(600, masterRealm.get("cors-max-age").asInt());
        assertEquals("X-Custom", masterRealm.get("cors-allowed-headers").asString());
        assertEquals("PUT,POST,DELETE,GET", masterRealm.get("cors-allowed-methods").asString());
        assertFalse(masterRealm.get("expose-token").asBoolean());
        assertFalse(masterRealm.get("always-refresh-token").asBoolean());
        assertTrue(masterRealm.get("register-node-at-startup").asBoolean());
        assertEquals(60, masterRealm.get("register-node-period").asInt());
        assertEquals("session", masterRealm.get("token-store").asString());
        assertEquals("sub", masterRealm.get("principal-attribute").asString());
        assertEquals("http://localhost:9000", masterRealm.get("proxy-url").asString());

        ModelNode jbossInfraRealm = newSubsystem.get("realm", "jboss-infra");
        assertTrue(jbossInfraRealm.isDefined());
        assertEquals("http://localhost:8080/auth", jbossInfraRealm.get("auth-server-url").asString());
        assertEquals("Content-Encoding", jbossInfraRealm.get("cors-exposed-headers").asString());
        assertTrue(jbossInfraRealm.get("autodetect-bearer-only").asBoolean());
        assertFalse(jbossInfraRealm.get("ignore-oauth-query-parameter").asBoolean());
        assertFalse(jbossInfraRealm.get("verify-token-audience").asBoolean());

        ModelNode secretCredentialDeployment = newSubsystem.get("secure-deployment", "secret-credential-app");
        assertTrue(secretCredentialDeployment.isDefined());
        assertEquals("master", secretCredentialDeployment.get("realm").asString());
        assertEquals("secret-credential-app", secretCredentialDeployment.get("resource").asString());
        assertTrue(secretCredentialDeployment.get("use-resource-role-mappings").asBoolean());
        assertFalse(secretCredentialDeployment.get("turn-off-change-session-id-on-login").asBoolean());
        assertEquals(10, secretCredentialDeployment.get("token-minimum-time-to-live").asInt());
        assertEquals(20, secretCredentialDeployment.get("min-time-between-jwks-requests").asInt());
        assertEquals(3600, secretCredentialDeployment.get("public-key-cache-ttl").asInt());
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB", secretCredentialDeployment.get("realm-public-key").asString());
        assertEquals("http://localhost:8080/auth", secretCredentialDeployment.get("auth-server-url").asString());
        assertEquals("EXTERNAL", secretCredentialDeployment.get("ssl-required").asString());
        assertEquals(443, secretCredentialDeployment.get("confidential-port").asInt());
        assertEquals("http://localhost:9000", secretCredentialDeployment.get("proxy-url").asString());
        assertTrue(secretCredentialDeployment.get("verify-token-audience").asBoolean());
        assertEquals("0aa31d98-e0aa-404c-b6e0-e771dba1e798", secretCredentialDeployment.get("credential", "secret").get("secret").asString());
        assertEquals("api/$1/", secretCredentialDeployment.get("redirect-rewrite-rule", "^/wsmaster/api/(.*)$").get("replacement").asString());

        ModelNode jwtCredentialDeployment = newSubsystem.get("secure-deployment", "jwt-credential-app");
        assertTrue(jwtCredentialDeployment.isDefined());
        assertEquals("master", jwtCredentialDeployment.get("realm").asString());
        assertEquals("jwt-credential-app", jwtCredentialDeployment.get("resource").asString());
        assertTrue(jwtCredentialDeployment.get("use-resource-role-mappings").asBoolean());
        assertEquals("/", jwtCredentialDeployment.get("adapter-state-cookie-path").asString());
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB", jwtCredentialDeployment.get("realm-public-key").asString());
        assertEquals("http://localhost:8080/auth", jwtCredentialDeployment.get("auth-server-url").asString());

        ModelNode jwtCredential = jwtCredentialDeployment.get("credential", "jwt");
        assertEquals("/tmp/keystore.jks", jwtCredential.get("client-keystore-file").asString());
        assertEquals("keyPassword", jwtCredential.get("client-key-password").asString());
        assertEquals("keystorePassword", jwtCredential.get("client-keystore-password").asString());
        assertEquals("keyAlias", jwtCredential.get("client-key-alias").asString());
        assertEquals("JKS", jwtCredential.get("client-keystore-type").asString());
        assertEquals("api/$1/", secretCredentialDeployment.get("redirect-rewrite-rule", "^/wsmaster/api/(.*)$").get("replacement").asString());

        ModelNode secureServer = newSubsystem.get("secure-server");
        assertFalse(secureServer.isDefined());
    }

    @Test
    public void testMigrateNonEmptyKeycloakConfigWithWarnings() throws Exception {
        String subsystemXml = readResource("keycloak-subsystem-migration-with-warnings-config.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

        ModelNode response = services.executeOperation(migrateOp);

        checkOutcome(response);

        ModelNode warnings = response.get(RESULT, "migration-warnings");
        assertEquals(warnings.toString(), 2, warnings.asList().size());
        assertTrue(warnings.get(0).toString().contains(("secure-server")));
        assertTrue(warnings.get(1).toString().contains(("secure-server")));

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME).isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, ELYTRON_OIDC_SUBSYSTEM_NAME);

        ModelNode masterRealm = newSubsystem.get("realm", "master");
        assertTrue(masterRealm.isDefined());

        ModelNode jbossInfraRealm = newSubsystem.get("realm", "jboss-infra");
        assertTrue(jbossInfraRealm.isDefined());

        ModelNode secureDeployment = newSubsystem.get("secure-deployment", "web-console");
        assertTrue(secureDeployment.isDefined());

        ModelNode secureServer = newSubsystem.get("secure-server");
        assertFalse(secureServer.isDefined());
    }

    private static class NewSubsystemAdditionalInitialization extends AdditionalInitialization {
        ElytronOidcExtension newSubsystem = new ElytronOidcExtension();
        boolean extensionAdded = false;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            final OperationDefinition removeExtension = new SimpleOperationDefinitionBuilder("remove", new StandardResourceDescriptionResolver("test", "test", getClass().getClassLoader()))
                    .build();
            PathElement keycloakExtension = PathElement.pathElement(EXTENSION, "org.keycloak.keycloak-adapter-subsystem");
            rootRegistration.registerSubModel(new SimpleResourceDefinition(keycloakExtension, ControllerResolver.getResolver(EXTENSION)))
                    .registerOperationHandler(removeExtension, new ReloadRequiredRemoveStepHandler());
            rootResource.registerChild(keycloakExtension, Resource.Factory.create());
            PathElement elytronExtension = PathElement.pathElement(EXTENSION, "org.wildfly.extension.elytron");
            rootRegistration.registerSubModel(new SimpleResourceDefinition(elytronExtension, ControllerResolver.getResolver(EXTENSION)))
                    .registerOperationHandler(removeExtension, new ReloadRequiredRemoveStepHandler());
            rootResource.registerChild(elytronExtension, Resource.Factory.create());

            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (! extensionAdded) {
                        extensionAdded = true;
                        newSubsystem.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.elytron-oidc-client",
                                rootRegistration, ExtensionRegistryType.SERVER));
                    }
                }
            }, null));
            registerCapabilities(capabilityRegistry, "org.wildfly.security.elytron");
        }

        @Override
        protected ProcessType getProcessType() {
            return ProcessType.HOST_CONTROLLER;
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }
    }
}
