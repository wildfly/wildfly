/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.metrics;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.metrics.MetricsSubsystemDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileMetricsSubsystemDefinition extends ModelOnlyResourceDefinition {

    static final String MP_CONFIG = "org.wildfly.microprofile.config";
    static final String METRICS_HTTP_CONTEXT_CAPABILITY = "org.wildfly.extension.metrics.http-context";

    static final RuntimeCapability<Void> MICROPROFILE_METRICS_HTTP_SECURITY_CAPABILITY =
            RuntimeCapability.Builder.of(MetricsSubsystemDefinition.METRICS_HTTP_SECURITY_CAPABILITY, Boolean.class)
                    .build();
    static final RuntimeCapability<Void> MICROPROFILE_METRICS_SCAN =
            RuntimeCapability.Builder.of(MetricsSubsystemDefinition.METRICS_SCAN_CAPABILITY)
                    .addRequirements(METRICS_HTTP_CONTEXT_CAPABILITY, MP_CONFIG)
                    .build();

    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final StringListAttributeDefinition EXPOSED_SUBSYSTEMS = new StringListAttributeDefinition.Builder("exposed-subsystems")
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final AttributeDefinition PREFIX = SimpleAttributeDefinitionBuilder.create("prefix", ModelType.STRING)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {SECURITY_ENABLED, EXPOSED_SUBSYSTEMS, PREFIX};

    protected MicroProfileMetricsSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(MicroProfileMetricsExtension.SUBSYSTEM_PATH,
                MicroProfileMetricsExtension.getResourceDescriptionResolver(MicroProfileMetricsExtension.SUBSYSTEM_NAME))
                .setAddHandler(new ModelOnlyAddStepHandler(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(MICROPROFILE_METRICS_HTTP_SECURITY_CAPABILITY, MICROPROFILE_METRICS_SCAN));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition att : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(att, null,
                    new ReloadRequiredWriteAttributeHandler(ATTRIBUTES));
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        MigrateOperation.registerOperations(resourceRegistration, getResourceDescriptionResolver());
    }
}
