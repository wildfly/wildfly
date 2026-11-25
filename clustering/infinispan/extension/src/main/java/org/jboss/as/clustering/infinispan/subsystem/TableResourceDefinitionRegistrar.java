/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder.StringTableManipulationConfigurationBuilder;

/**
 * Registers a resource definition for the table component of a JDBC cache store.
 * @author Paul Ferraro
 */
public class TableResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<TableManipulationConfiguration, StringTableManipulationConfigurationBuilder> {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("table", "string"));
    static final BinaryServiceDescriptor<TableManipulationConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(List.of(StoreResourceRegistration.WILDCARD, REGISTRATION), TableManipulationConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT).setAllowMultipleRegistrations(true).build();

    enum Attribute implements AttributeDefinitionProvider {
        PREFIX("prefix", ModelType.STRING, new ModelNode("ispn_entry")),
        FETCH_SIZE("fetch-size", ModelType.INT, new ModelNode(100)),
        CREATE_ON_START("create-on-start", ModelType.BOOLEAN, ModelNode.TRUE),
        DROP_ON_STOP("drop-on-stop", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    enum ColumnAttribute implements AttributeDefinitionProvider {
        ID("id-column", "id", "VARCHAR"),
        DATA("data-column", "datum", "BINARY"),
        SEGMENT("segment-column", "segment", "INTEGER"),
        TIMESTAMP("timestamp-column", "version", "BIGINT"),
        ;
        private final AttributeDefinition name;
        private final AttributeDefinition type;
        private final AttributeDefinition definition;

        ColumnAttribute(String name, String defaultName, String defaultType) {
            this.name = new SimpleAttributeDefinitionBuilder("name", ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultName))
                    .build();
            this.type = new SimpleAttributeDefinitionBuilder("type", ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultType))
                    .build();
            this.definition = ObjectTypeAttributeDefinition.Builder.of(name, this.name, this.type)
                    .setRequired(false)
                    .setSuffix("column")
                    .build();
        }

        AttributeDefinition getColumnName() {
            return this.name;
        }

        AttributeDefinition getColumnType() {
            return this.type;
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    TableResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return REGISTRATION;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .provideAttributes(EnumSet.allOf(Attribute.class))
                .provideAttributes(EnumSet.allOf(ColumnAttribute.class));
    }

    @Override
    public ServiceDependency<StringTableManipulationConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Map<ColumnAttribute, Map.Entry<String, String>> columns = new EnumMap<>(ColumnAttribute.class);
        for (ColumnAttribute column : EnumSet.allOf(ColumnAttribute.class)) {
            ModelNode columnModel = column.resolveModelAttribute(context, model);
            String name = column.getColumnName().resolveModelAttribute(context, columnModel).asString();
            String type = column.getColumnType().resolveModelAttribute(context, columnModel).asString();
            columns.put(column, Map.entry(name, type));
        }

        int fetchSize = Attribute.FETCH_SIZE.resolveModelAttribute(context, model).asInt();
        String prefix = Attribute.PREFIX.resolveModelAttribute(context, model).asString();
        boolean createOnStart = Attribute.CREATE_ON_START.resolveModelAttribute(context, model).asBoolean();
        boolean dropOnStop = Attribute.DROP_ON_STOP.resolveModelAttribute(context, model).asBoolean();

        return ServiceDependency.from(new Supplier<>() {
            @Override
            public StringTableManipulationConfigurationBuilder get() {
                return new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class).table()
                        .createOnStart(createOnStart)
                        .dropOnExit(dropOnStop)
                        .idColumnName(columns.get(ColumnAttribute.ID).getKey())
                        .idColumnType(columns.get(ColumnAttribute.ID).getValue())
                        .dataColumnName(columns.get(ColumnAttribute.DATA).getKey())
                        .dataColumnType(columns.get(ColumnAttribute.DATA).getValue())
                        .segmentColumnName(columns.get(ColumnAttribute.SEGMENT).getKey())
                        .segmentColumnType(columns.get(ColumnAttribute.SEGMENT).getValue())
                        .timestampColumnName(columns.get(ColumnAttribute.TIMESTAMP).getKey())
                        .timestampColumnType(columns.get(ColumnAttribute.TIMESTAMP).getValue())
                        .fetchSize(fetchSize)
                        .tableNamePrefix(prefix)
                        ;
            }
        });
    }
}
