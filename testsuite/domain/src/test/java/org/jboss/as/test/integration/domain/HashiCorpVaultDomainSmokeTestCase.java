/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.util.AssumeTestGroupUtil.assumeDockerAvailable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * Checks profile configuration, credential operations and managed-server restart with a real Vault instance.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HashiCorpVaultDomainSmokeTestCase {

    private static final String VAULT_TOKEN = "domain-smoke-token";
    private static final String STORE_NAME = "domain-vault-store";
    private static final String ALIAS = "domain-smoke?password";
    private static final String NORMALIZED_ALIAS = "#" + ALIAS;
    private static final String SECRET = "domain-smoke-password";
    private static final PathAddress PROFILE = PathAddress.pathAddress("profile", "default");
    private static final PathAddress SERVER_CONFIG = PathAddress.pathAddress("host", "primary")
            .append("server-config", "main-one");
    private static final PathAddress SERVER = PathAddress.pathAddress("host", "primary").append("server", "main-one");
    private static final PathAddress PROFILE_SUBSYSTEM = PROFILE.append("subsystem", "hashicorp-vault");
    private static final PathAddress PROFILE_STORE = PROFILE_SUBSYSTEM.append("credential-store", STORE_NAME);
    private static final PathAddress SERVER_STORE = SERVER.append("subsystem", "hashicorp-vault")
            .append("credential-store", STORE_NAME);

    @Test
    public void testVaultCredentialStoreInManagedServer() throws Exception {
        assumeDockerAvailable();

        try (VaultContainer<?> vault = new VaultContainer<>(DockerImageName.parse(System.getProperty("vault.docker.image")))
                .withVaultToken(VAULT_TOKEN)) {
            vault.start();

            DomainTestSupport.Configuration configuration = DomainTestSupport.Configuration.create(
                    getClass().getSimpleName(), "domain-configs/domain-minimal.xml", "host-configs/host-primary.xml", null);
            configuration.getPrimaryConfiguration().setStability(Stability.DEFAULT);
            try (DomainTestSupport support = DomainTestSupport.create(configuration)) {
                support.start();
                DomainLifecycleUtil lifecycle = support.getDomainPrimaryLifecycleUtil();
                ModelControllerClient client = lifecycle.getDomainClient();
                assertServerEnvironment(client);

                try (ManagementResources resources = new ManagementResources(client)) {
                    resources.add(Util.createAddOperation(PathAddress.pathAddress("extension", "org.wildfly.extension.hashicorp-vault")));
                    resources.add(Util.createAddOperation(PROFILE_SUBSYSTEM));

                    ModelNode addStore = Util.createAddOperation(PROFILE_STORE);
                    addStore.get("host-address").set(vault.getHttpHostAddress());
                    addStore.get("credential-reference", "clear-text").set(VAULT_TOKEN);
                    resources.add(addStore);
                    assertStoreConfiguration(client, vault.getHttpHostAddress());

                    ModelNode addAlias = Util.createEmptyOperation("add-alias", SERVER_STORE);
                    addAlias.get("alias").set(ALIAS);
                    addAlias.get("secret-value").set(SECRET);
                    ModelNode removeAlias = Util.createEmptyOperation("remove-alias", SERVER_STORE);
                    removeAlias.get("alias").set(ALIAS);
                    resources.execute(addAlias, removeAlias);
                    assertAliasPresent(client, true);

                    HttpClient http = HttpClient.newBuilder()
                            .connectTimeout(TimeoutUtil.adjust(Duration.ofSeconds(10))).build();
                    HttpResponse<String> response = readVaultSecret(http, vault);
                    assertEquals("Vault secret read: " + response.body(), 200, response.statusCode());
                    ModelNode secret = ModelNode.fromJSONString(response.body());
                    assertTrue("Missing Vault secret: " + secret, secret.hasDefined("data", "data", "password"));
                    assertEquals(SECRET, secret.get("data", "data", "password").asString());

                    ModelNode restart = Util.createEmptyOperation("restart", SERVER_CONFIG);
                    restart.get("blocking").set(true);
                    executeSuccess(client, restart);
                    lifecycle.awaitServers(System.currentTimeMillis());
                    assertServerEnvironment(client);
                    assertStoreConfiguration(client, vault.getHttpHostAddress());
                    assertAliasPresent(client, true);

                    // Remove the alias before the profile resources on the cleanup stack.
                    resources.removeLastAdded();
                    assertAliasPresent(client, false);
                    response = readVaultSecret(http, vault);
                    assertEquals("Removed Vault secret: " + response.body(), 404, response.statusCode());
                }

                ModelNode stores = Util.createEmptyOperation("read-children-names", SERVER);
                stores.get("child-type").set("subsystem");
                assertFalse("Vault subsystem remains in the managed server",
                        executeSuccess(client, stores).asList().contains(new ModelNode("hashicorp-vault")));
            }
        }
    }

    private static void assertServerEnvironment(ModelControllerClient client) throws IOException {
        ModelNode state = Util.getReadAttributeOperation(SERVER, "server-state");
        assertEquals("running", executeSuccess(client, state).asString());
        PathAddress environment = SERVER.append("core-service", "server-environment");
        assertEquals("DOMAIN", executeSuccess(client, Util.getReadAttributeOperation(environment, "launch-type")).asString());
        assertEquals(Stability.DEFAULT.toString(),
                executeSuccess(client, Util.getReadAttributeOperation(environment, "stability")).asString());
    }

    private static void assertStoreConfiguration(ModelControllerClient client, String vaultAddress) throws IOException {
        for (PathAddress address : List.of(PROFILE_STORE, SERVER_STORE)) {
            ModelNode store = executeSuccess(client, Util.createEmptyOperation("read-resource", address));
            assertEquals(address.toString(), vaultAddress, store.get("host-address").asString());
        }
    }

    private static void assertAliasPresent(ModelControllerClient client, boolean present) throws IOException {
        ModelNode read = Util.createEmptyOperation("read-aliases", SERVER_STORE);
        read.get("recursive").set(true);
        List<ModelNode> aliases = executeSuccess(client, read).asList();
        assertEquals("Managed-server aliases: " + aliases, present, aliases.contains(new ModelNode(NORMALIZED_ALIAS)));
    }

    private static HttpResponse<String> readVaultSecret(HttpClient http, VaultContainer<?> vault) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(vault.getHttpHostAddress() + "/v1/secret/data/domain-smoke"))
                .timeout(TimeoutUtil.adjust(Duration.ofSeconds(30)))
                .header("X-Vault-Token", VAULT_TOKEN).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static ModelNode executeSuccess(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode response = client.execute(operation);
        assertEquals(operation + ": " + response, SUCCESS, response.get(OUTCOME).asString());
        return response.get(RESULT);
    }

    /**
     * Reverses successful changes even after partial setup, preserving test failures through try-with-resources.
     */
    private static final class ManagementResources implements AutoCloseable {
        private final ModelControllerClient client;
        private final Deque<ModelNode> removals = new ArrayDeque<>();

        private ManagementResources(ModelControllerClient client) {
            this.client = client;
        }

        private void add(ModelNode operation) throws IOException {
            ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(operation.get("address")));
            remove.get("operation-headers", "allow-resource-service-restart").set(true);
            execute(operation, remove);
        }

        private void execute(ModelNode operation, ModelNode cleanup) throws IOException {
            executeSuccess(client, operation);
            removals.push(cleanup);
        }

        private void removeLastAdded() throws IOException {
            executeSuccess(client, removals.peek());
            removals.pop();
        }

        @Override
        public void close() throws Exception {
            List<Throwable> failures = new ArrayList<>();
            while (!removals.isEmpty()) {
                ModelNode remove = removals.pop();
                try {
                    executeSuccess(client, remove);
                    if ("remove".equals(remove.get("operation").asString())) {
                        PathAddress address = PathAddress.pathAddress(remove.get("address"));
                        PathElement resource = address.getLastElement();
                        ModelNode read = Util.createEmptyOperation("read-children-names", address.getParent());
                        read.get("child-type").set(resource.getKey());
                        assertFalse("Resource remains after removal: " + address,
                                executeSuccess(client, read).asList().contains(new ModelNode(resource.getValue())));
                    }
                } catch (Exception | AssertionError e) {
                    failures.add(e);
                }
            }
            MultipleFailureException.assertEmpty(failures);
        }
    }
}
