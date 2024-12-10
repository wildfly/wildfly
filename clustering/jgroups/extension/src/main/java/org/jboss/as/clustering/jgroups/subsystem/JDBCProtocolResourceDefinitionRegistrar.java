/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.sql.DataSource;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.JDBC_PING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a shared database discovery protocol, i.e. JDBC_PING.
 * @author Paul Ferraro
 */
public class JDBCProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<JDBC_PING> {

    JDBCProtocolResourceDefinitionRegistrar(JDBCProtocolResourceDescription description, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceDescriptorConfigurator<>() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return description;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolResourceDescriptorConfigurator.super.apply(builder)
                        .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(List.of(JDBCProtocolResourceDescription.DATA_SOURCE)))
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                        ;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<JDBC_PING>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ProtocolResourceDescriptorConfigurator.super.resolve(context, model).combine(JDBCProtocolResourceDescription.DATA_SOURCE.resolve(context, model), new BiFunction<>() {
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
        });
    }
}
