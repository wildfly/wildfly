/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.function.BiFunction;

import org.jboss.as.clustering.controller.AdminOnlyOperationStepHandlerTransformer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HostSingleSignOnDefinition extends SingleSignOnDefinition {

    public static final RuntimeCapability<Void> HOST_SSO_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HOST_SSO, true)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT)
            .build();

    public HostSingleSignOnDefinition() {
        super(new BiFunction<>() {
            @Override
            public ResourceDefinition.Builder apply(ResourceRegistration registration, ResourceDescriptionResolver resolver) {
                return ResourceDefinition.builder(registration, resolver, UndertowSubsystemModel.VERSION_12_0_0);
            }
        }, new BiFunction<>() {
            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder, ResourceServiceConfigurator configurator) {
                return builder.addCapability(HOST_SSO_CAPABILITY)
                        .withAddResourceOperationTransformation(AdminOnlyOperationStepHandlerTransformer.INSTANCE)
                        ;
            }
        });
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return ResourceServiceInstaller.combine();
    }
}
