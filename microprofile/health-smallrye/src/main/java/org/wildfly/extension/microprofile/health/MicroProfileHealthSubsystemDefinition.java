/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.health.HealthSubsystemDefinition;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthSubsystemDefinition extends PersistentResourceDefinition {

    private static final String[] ALLOWED_STATUS = {"UP", "DOWN"};
    static final String HEALTH_HTTP_CONTEXT_CAPABILITY = "org.wildfly.extension.health.http-context";
    static final String HEALTH_SERVER_PROBE_CAPABILITY = "org.wildfly.extension.health.server-probes";

    static final RuntimeCapability<Void> HEALTH_REPORTER_RUNTIME_CAPABILITY = RuntimeCapability.Builder.of(MicroProfileHealthReporter.SERVICE_DESCRIPTOR)
            .addRequirements(WELD_CAPABILITY_NAME, HEALTH_SERVER_PROBE_CAPABILITY)
            .build();

    static final RuntimeCapability<Void> MICROPROFILE_HEALTH_HTTP_SECURITY_CAPABILITY = RuntimeCapability.Builder.of(HealthSubsystemDefinition.HEALTH_HTTP_SECURITY_CAPABILITY, Boolean.class)
            .build();

    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition EMPTY_LIVENESS_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-liveness-checks-status", ModelType.STRING)
            .setDefaultValue(new ModelNode("UP"))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_STATUS))
            .build();

    static final AttributeDefinition EMPTY_READINESS_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-readiness-checks-status", ModelType.STRING)
            .setDefaultValue(new ModelNode("UP"))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_STATUS))
            .build();

    static final AttributeDefinition EMPTY_STARTUP_CHECKS_STATUS = SimpleAttributeDefinitionBuilder.create("empty-startup-checks-status", ModelType.STRING)
            .setDefaultValue(new ModelNode("UP"))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_STATUS))
            .build();


    static final AttributeDefinition[] ATTRIBUTES = { SECURITY_ENABLED, EMPTY_LIVENESS_CHECKS_STATUS, EMPTY_READINESS_CHECKS_STATUS, EMPTY_STARTUP_CHECKS_STATUS};
    private final AtomicReference<MicroProfileHealthReporter> reporter;
    private final boolean registerRuntimeOperations;

    MicroProfileHealthSubsystemDefinition(boolean registerRuntimeOperations) {
        this(registerRuntimeOperations, new AtomicReference<>());
    }

    private MicroProfileHealthSubsystemDefinition(boolean registerRuntimeOperations, AtomicReference<MicroProfileHealthReporter> reporter) {
        super(new Parameters(MicroProfileHealthExtension.SUBSYSTEM_PATH,
                MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new MicroProfileHealthSubsystemAdd(reporter::set))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(HEALTH_REPORTER_RUNTIME_CAPABILITY, MICROPROFILE_HEALTH_HTTP_SECURITY_CAPABILITY));
        this.registerRuntimeOperations = registerRuntimeOperations;
        this.reporter = reporter;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (registerRuntimeOperations) {
            new RuntimeOperationStepHandler<>(new CheckOperationExecutor(this.reporter::get), CheckOperation.class).register(resourceRegistration);
        }
    }
}
