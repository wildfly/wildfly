/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.AUTH;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class AuthProtocolResourceDefinition extends ProtocolResourceDefinition<AUTH> {

    static {
        ClassConfigurator.add((short) 1100, BinaryAuthToken.class);
        ClassConfigurator.add((short) 1101, CipherAuthToken.class);
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation("auth_class"))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    .addResourceCapabilityReference(ResourceCapabilityReferenceRecorder.builder(CAPABILITY, AuthTokenResourceDefinition.SERVICE_DESCRIPTOR).build())
                    ;
        }
    }

    AuthProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), parentServiceConfigurator);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PlainAuthTokenResourceDefinition().register(registration);
        new DigestAuthTokenResourceDefinition().register(registration);
        new CipherAuthTokenResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<AUTH>, ProtocolConfiguration<AUTH>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<AuthToken> token = ServiceDependency.on(AuthTokenResourceDefinition.SERVICE_DESCRIPTOR, context.getCurrentAddress().getParent().getLastElement().getValue(), context.getCurrentAddressValue());
        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<AUTH> apply(ProtocolConfiguration<AUTH> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public AUTH createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        return super.createProtocol(stackConfiguration).setAuthToken(token.get());
                    }
                };
            }
        }, token);
    }
}
