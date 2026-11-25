/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.common.function.Functions;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JdrReportSubsystemDefinition extends SimpleResourceDefinition {

    static final RuntimeCapability<Void> JDR_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jdr", JdrReportCollector.class)
            //.addRequirements(ModelControllerClientFactory.SERVICE_DESCRIPTOR, Capabilities.MANAGEMENT_EXECUTOR) TODO determine why this breaks domain mode provisioning
            .build();

    private final AtomicReference<JdrReportCollector> collectorReference;

    JdrReportSubsystemDefinition(AtomicReference<JdrReportCollector> collectorReference) {
        super(getParameters(collectorReference));
        this.collectorReference = collectorReference;
    }

    private static Parameters getParameters(AtomicReference<JdrReportCollector> collectorReference) {
        Consumer<JdrReportCollector> subsystemConsumer = collectorReference == null ? Functions.discardingConsumer() : collectorReference::set;
        OperationStepHandler addHandler = new JdrReportSubsystemAdd(subsystemConsumer);
        return new Parameters(JdrReportExtension.SUBSYSTEM_PATH, JdrReportExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(addHandler)
                .setRemoveHandler(JdrReportSubsystemRemove.INSTANCE)
                .setCapabilities(JDR_CAPABILITY);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        if (collectorReference != null) {
            resourceRegistration.registerOperationHandler(JdrReportRequestHandler.DEFINITION, new JdrReportRequestHandler(collectorReference::get));
        }
    }
}
