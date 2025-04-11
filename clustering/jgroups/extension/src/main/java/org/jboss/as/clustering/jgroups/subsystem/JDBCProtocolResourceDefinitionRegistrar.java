/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.JDBC_PING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a shared database discovery protocol, i.e. JDBC_PING.
 * @author Paul Ferraro
 */
public class JDBCProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<JDBC_PING> {
    enum Protocol implements ResourceRegistration {
        JDBC_PING;

        private final PathElement path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(this.name());

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static final CapabilityReferenceAttributeDefinition<DataSource> DATA_SOURCE = new CapabilityReferenceAttributeDefinition.Builder<>("data-source", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build()).build();

    JDBCProtocolResourceDefinitionRegistrar(Protocol registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
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
                .addAttributes(List.of(DATA_SOURCE))
                .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(List.of(DATA_SOURCE)))
                .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                ;
    }

    @Override
    public ServiceDependency<ProtocolConfiguration<JDBC_PING>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return super.resolve(context, model).combine(DATA_SOURCE.resolve(context, model), new BiFunction<>() {
            @Override
            public ProtocolConfiguration<JDBC_PING> apply(ProtocolConfiguration<JDBC_PING> config, DataSource dataSource) {
                return new ProtocolConfigurationDecorator<>(config) {
                    @Override
                    public JDBC_PING createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                        return super.createProtocol(stackConfiguration).setDataSource(dataSource);
                    }
                };
            }
        });
    }
}
