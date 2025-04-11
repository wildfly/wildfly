/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.AUTH;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an AUTH protocol.
 * @author Paul Ferraro
 */
public class AuthProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<AUTH> {
    enum Protocol implements ResourceRegistration {
        AUTH;

        private final PathElement path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement("AUTH");

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static {
        ClassConfigurator.add((short) 1100, BinaryAuthToken.class);
        ClassConfigurator.add((short) 1101, CipherAuthToken.class);
    }

    private static final ResourceCapabilityReference<AuthToken> TOKEN = ResourceCapabilityReference.builder(AbstractProtocolResourceDefinitionRegistrar.CAPABILITY, AuthTokenResourceDefinitionRegistrar.SERVICE_DESCRIPTOR).build();

    AuthProtocolResourceDefinitionRegistrar(Protocol registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return registration;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addResourceCapabilityReference(TOKEN)
                .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation("auth_class"))
                .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new PlainAuthTokenResourceDefinitionRegistrar().register(registration, context);
        new DigestAuthTokenResourceDefinitionRegistrar().register(registration, context);
        new CipherAuthTokenResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ServiceDependency<ProtocolConfiguration<AUTH>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return super.resolve(context, model).combine(TOKEN.resolve(context, model), new BiFunction<>() {
            @Override
            public ProtocolConfiguration<AUTH> apply(ProtocolConfiguration<AUTH> config, AuthToken token) {
                return new ProtocolConfigurationDecorator<>(config) {
                    @Override
                    public AUTH createProtocol(ChannelFactoryConfiguration configuration) {
                        return super.createProtocol(configuration).setAuthToken(token);
                    }
                };
            }
        });
    }
}
