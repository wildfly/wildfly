/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.JDBC_PING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource definition override for protocols that require a JDBC DataSource.
 * @author Paul Ferraro
 */
public class JDBCProtocolResourceDefinition extends ProtocolResourceDefinition<JDBC_PING> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DATA_SOURCE("data-source", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private static final class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation(Attribute.class))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    ;
        }
    }

    JDBCProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), parentServiceConfigurator);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<JDBC_PING>, ProtocolConfiguration<JDBC_PING>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<DataSource> dataSource = ServiceDependency.on(CommonServiceDescriptor.DATA_SOURCE, Attribute.DATA_SOURCE.resolveModelAttribute(context, model).asString());
        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<JDBC_PING> apply(ProtocolConfiguration<JDBC_PING> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public JDBC_PING createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        return super.createProtocol(stackConfiguration).setDataSource(dataSource.get());
                    }
                };
            }
        }, dataSource);
    }
}
