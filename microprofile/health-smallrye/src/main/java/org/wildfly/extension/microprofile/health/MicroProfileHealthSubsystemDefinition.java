/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.health.HealthSubsystemDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthSubsystemDefinition extends PersistentResourceDefinition {

    private static final String[] ALLOWED_STATUS = {"UP", "DOWN"};
    static final String MICROPROFILE_HEALTH_REPORTER_CAPABILITY = "org.wildfly.extension.microprofile.health.reporter";
    static final String HEALTH_HTTP_CONTEXT_CAPABILITY = "org.wildfly.extension.health.http-context";
    static final String HEALTH_SERVER_PROBE_CAPABILITY = "org.wildfly.extension.health.server-probes";

    static final RuntimeCapability<Void> HEALTH_REPORTER_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROPROFILE_HEALTH_REPORTER_CAPABILITY, MicroProfileHealthReporter.class)
                    .addRequirements(WELD_CAPABILITY_NAME, HEALTH_SERVER_PROBE_CAPABILITY)
                    .build();

    public static final ServiceName HEALTH_REPORTER_SERVICE = ServiceName.parse(MICROPROFILE_HEALTH_REPORTER_CAPABILITY);

    static final RuntimeCapability<Void> MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.microprofile.health.http-context", MicroProfileHealthContextService.class)
            .addRequirements(HEALTH_HTTP_CONTEXT_CAPABILITY)
            .build();
    static final ServiceName HTTP_CONTEXT_SERVICE = MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName();

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
    private boolean registerRuntimeOperations;

    protected MicroProfileHealthSubsystemDefinition(boolean registerRuntimeOperations) {
        super(new Parameters(MicroProfileHealthExtension.SUBSYSTEM_PATH,
                MicroProfileHealthExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(MicroProfileHealthSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(MicroProfileHealthSubsystemAdd.INSTANCE))
                .setCapabilities(HEALTH_REPORTER_RUNTIME_CAPABILITY, MICROPROFILE_HEALTH_HTTP_CONTEXT_CAPABILITY, MICROPROFILE_HEALTH_HTTP_SECURITY_CAPABILITY));
        this.registerRuntimeOperations = registerRuntimeOperations;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (registerRuntimeOperations) {
            CheckOperations.register(resourceRegistration);
        }
    }


}
