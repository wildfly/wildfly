/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistrar;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;

/**
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceResourceDefinition extends SubsystemResourceDefinition {

    static final PathElement PATH = pathElement(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME);

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        MICROPROFILE_FAULT_TOLERANCE("org.wildfly.microprofile.fault-tolerance"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name)
                    .addRequirements("org.wildfly.microprofile.config")
                    .build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    protected MicroProfileFaultToleranceResourceDefinition() {
        super(PATH, MicroProfileFaultToleranceExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                ;
        MicroProfileFaultToleranceServiceHandler handler = new MicroProfileFaultToleranceServiceHandler();

        new DeploymentChainContributingResourceRegistrar(descriptor, handler, handler).register(registration);
    }

}
