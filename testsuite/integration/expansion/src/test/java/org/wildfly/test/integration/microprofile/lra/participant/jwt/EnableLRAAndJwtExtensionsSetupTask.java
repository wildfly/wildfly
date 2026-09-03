/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.integration.microprofile.jwt.TokenUtil;

/**
 * Setup task that enables both LRA extensions and MicroProfile JWT subsystem.
 * This is required for testing JWT propagation through LRA operations.
 *
 * <p>The LRA coordinator resolves its JWT configuration ({@code lra.http-client.providers} and
 * {@code lra.security.service-token.location}) and the {@code JwtTokenCallbackRequestFilter} provider
 * class through its own module classloader, not through the participant deployment. Those settings are
 * therefore exposed here as server system properties (picked up by the coordinator's MicroProfile Config
 * system-property source) rather than being packaged inside the participant WAR. The service token itself
 * is written to a file the coordinator can read via the {@code file://} scheme.</p>
 */
public class EnableLRAAndJwtExtensionsSetupTask extends SnapshotServerSetupTask {
    private static final String MODULE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String MODULE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String MODULE_JWT = "org.wildfly.extension.microprofile.jwt-smallrye";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";
    private static final String SUBSYSTEM_JWT = "microprofile-jwt-smallrye";

    private static final String PROP_HTTP_CLIENT_PROVIDERS = "lra.http-client.providers";
    private static final String PROP_SERVICE_TOKEN_LOCATION = "lra.security.service-token.location";
    private static final String CALLBACK_REQUEST_FILTER = "io.narayana.lra.coordinator.security.JwtTokenCallbackRequestFilter";
    private static final URL KEY_LOCATION = EnableLRAAndJwtExtensionsSetupTask.class.getResource("private.pem");

    private volatile Path serviceTokenFile;

    @Override
    protected void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
        final Set<String> extensions = listExtensions(managementClient);
        final Set<String> subsystems = listSubsystems(managementClient);
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

        // Add LRA Coordinator extension and subsystem
        if (!extensions.contains(MODULE_LRA_COORDINATOR)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", MODULE_LRA_COORDINATOR)));
        }
        if (!subsystems.contains(SUBSYSTEM_LRA_COORDINATOR)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("subsystem", SUBSYSTEM_LRA_COORDINATOR)));
        }

        // Add LRA Participant extension and subsystem
        if (!extensions.contains(MODULE_LRA_PARTICIPANT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", MODULE_LRA_PARTICIPANT)));
        }
        if (!subsystems.contains(SUBSYSTEM_LRA_PARTICIPANT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("subsystem", SUBSYSTEM_LRA_PARTICIPANT)));
        }

        // Add MicroProfile JWT extension and subsystem
        if (!extensions.contains(MODULE_JWT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", MODULE_JWT)));
        }
        if (!subsystems.contains(SUBSYSTEM_JWT)) {
            builder.addStep(Operations.createAddOperation(Operations.createAddress("subsystem", SUBSYSTEM_JWT)));
        }

        // Provide the coordinator with a service token to authenticate its participant callbacks
        // (@Complete/@Compensate). The coordinator has no inbound JWT when the LRA is closed directly
        // via the coordinator REST API, so it falls back to this pre-provisioned token.
        serviceTokenFile = Files.createTempFile("lra-service-token", ".jwt");
        Files.writeString(serviceTokenFile, generateServiceToken());

        // Register the coordinator-side JWT configuration as system properties so the coordinator module
        // picks them up through its MicroProfile Config system-property source.
        builder.addStep(addSystemProperty(PROP_HTTP_CLIENT_PROVIDERS, CALLBACK_REQUEST_FILTER));
        builder.addStep(addSystemProperty(PROP_SERVICE_TOKEN_LOCATION,
            "file://" + serviceTokenFile.toAbsolutePath()));

        executeOperation(managementClient, builder.build());
    }

    @Override
    protected void nonManagementCleanUp() throws Exception {
        if (serviceTokenFile != null) {
            Files.deleteIfExists(serviceTokenFile);
            serviceTokenFile = null;
        }
    }

    private static ModelNode addSystemProperty(final String name, final String value) {
        final ModelNode op = Operations.createAddOperation(Operations.createAddress("system-property", name));
        op.get("value").set(value);
        return op;
    }

    private static String generateServiceToken() throws Exception {
        final Supplier<PrivateKey> keySupplier = TokenUtil.createKeySupplier(
            Paths.get(KEY_LOCATION.toURI()).toAbsolutePath().toString());
        final String currentDate = java.time.LocalDate.now().toString();
        // Long-lived token representing the LRA coordinator service account used for participant callbacks.
        return TokenUtil.generateJWT(keySupplier, "lra-coordinator-service", currentDate, "ServiceAccount");
    }

    private Set<String> listExtensions(final ManagementClient client) throws IOException {
        final ModelNode op = Operations.createOperation("read-children-names");
        op.get(ClientConstants.CHILD_TYPE).set("extension");
        return executeOperation(client, op).asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
    }

    private Set<String> listSubsystems(final ManagementClient client) throws IOException {
        final ModelNode op = Operations.createOperation("read-children-names");
        op.get(ClientConstants.CHILD_TYPE).set("subsystem");
        return executeOperation(client, op).asList()
                .stream()
                .map(ModelNode::asString)
                .collect(Collectors.toSet());
    }
}
