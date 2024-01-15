/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinition;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinition;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

class MicrometerSubsystemDefinition extends SimpleResourceDefinition {
    private static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    private static final String MICROMETER_API_MODULE = "org.wildfly.micrometer.deployment";
    static final String CLIENT_FACTORY_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String MANAGEMENT_EXECUTOR = "org.wildfly.management.executor";
    static final String PROCESS_STATE_NOTIFIER = "org.wildfly.management.process-state-notifier";

    static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-collector", MicrometerCollector.class)
                    .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR, PROCESS_STATE_NOTIFIER)
                    .build();
    static final RuntimeCapability<Void> MICROMETER_REGISTRY_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".registry", WildFlyRegistry.class)
                    .build();
    static final ServiceName MICROMETER_COLLECTOR = MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName();
    static final String[] MODULES = {
    };

    static final String[] EXPORTED_MODULES = {
            MICROMETER_API_MODULE,
            "io.opentelemetry.otlp",
            "io.micrometer"
    };


    static final StringListAttributeDefinition EXPOSED_SUBSYSTEMS =
            new StringListAttributeDefinition.Builder("exposed-subsystems" )
                    .setDefaultValue(ModelNode.fromJSONString("[\"*\"]"))
                    .setRequired(false)
                    .setRestartAllServices()
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {
            EXPOSED_SUBSYSTEMS
    };
    private final WildFlyCompositeRegistry wildFlyRegistry;

    protected MicrometerSubsystemDefinition(WildFlyCompositeRegistry wildFlyRegistry) {
        super(new SimpleResourceDefinition.Parameters(MicrometerExtension.SUBSYSTEM_PATH,
                MicrometerExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new MicrometerSubsystemAdd(wildFlyRegistry))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new OtlpRegistryDefinition(wildFlyRegistry));
        resourceRegistration.registerSubModel(new PrometheusRegistryDefinition(wildFlyRegistry));
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.micrometer" )
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null,
                    ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }

}
