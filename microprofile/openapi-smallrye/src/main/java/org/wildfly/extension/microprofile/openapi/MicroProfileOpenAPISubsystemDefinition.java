/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Root resource definition for MicroProfile Open API subsystem
 * @author Michael Edgar
 */
public class MicroProfileOpenAPISubsystemDefinition extends SubsystemResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, MicroProfileOpenAPIExtension.SUBSYSTEM_NAME);

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        MICROPROFILE_OPENAPI("org.wildfly.microprofile.openapi"),
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

    MicroProfileOpenAPISubsystemDefinition() {
        super(PATH, MicroProfileOpenAPIExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                ;
        MicroProfileOpenAPIServiceHandler handler = new MicroProfileOpenAPIServiceHandler();
        new DeploymentChainContributingResourceRegistrar(descriptor, handler, handler).register(registration);
    }
}
