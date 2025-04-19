/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import java.util.function.Consumer;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.microprofile.openapi.deployment.OpenAPIDependencyProcessor;
import org.wildfly.extension.microprofile.openapi.deployment.OpenAPIDocumentProcessor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * Root resource definition for MicroProfile Open API subsystem
 * @author Michael Edgar
 */
public class MicroProfileOpenAPISubsystemRegistrar implements SubsystemResourceDefinitionRegistrar, Consumer<DeploymentProcessorTarget> {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("microprofile-openapi-smallrye");
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), MicroProfileOpenAPISubsystemRegistrar.class);

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.microprofile.openapi")
            .addRequirements("org.wildfly.microprofile.config")
            .build();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor = ResourceDescriptor.builder(SUBSYSTEM_RESOLVER)
                .addCapability(CAPABILITY)
                .withDeploymentChainContributor(this)
                .build();

        ResourceDefinition definition = ResourceDefinition.builder(REGISTRATION, descriptor.getResourceDescriptionResolver()).build();
        ManagementResourceRegistration registration = parent.registerSubsystemModel(definition);
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_OPENAPI, new OpenAPIDependencyProcessor());
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.INSTALL, Phase.POST_MODULE_MICROPROFILE_OPENAPI, new OpenAPIDocumentProcessor());
    }
}
