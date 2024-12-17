/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public abstract class TableResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String value) {
        return PathElement.pathElement("table", value);
    }

    static final BinaryServiceDescriptor<TableManipulationConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(StoreResourceDefinition.SERVICE_DESCRIPTOR.getName() + "." + WILDCARD_PATH.getKey(), TableManipulationConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).setDynamicNameMapper(BinaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
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
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum ColumnAttribute implements org.jboss.as.clustering.controller.Attribute {
        ID("id-column", "id", "VARCHAR"),
        DATA("data-column", "datum", "BINARY"),
        SEGMENT("segment-column", "segment", "INTEGER"),
        TIMESTAMP("timestamp-column", "version", "BIGINT"),
        ;
        private final org.jboss.as.clustering.controller.Attribute name;
        private final org.jboss.as.clustering.controller.Attribute type;
        private final AttributeDefinition definition;

        ColumnAttribute(String name, String defaultName, String defaultType) {
            this.name = new SimpleAttribute(new SimpleAttributeDefinitionBuilder("name", ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultName))
                    .build());
            this.type = new SimpleAttribute(new SimpleAttributeDefinitionBuilder("type", ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultType))
                    .build());
            this.definition = ObjectTypeAttributeDefinition.Builder.of(name, this.name.getDefinition(), this.type.getDefinition())
                    .setRequired(false)
                    .setSuffix("column")
                    .build();
        }

        org.jboss.as.clustering.controller.Attribute getColumnName() {
            return this.name;
        }

        org.jboss.as.clustering.controller.Attribute getColumnType() {
            return this.type;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final org.jboss.as.clustering.controller.Attribute prefixAttribute;

    TableResourceDefinition(PathElement path, org.jboss.as.clustering.controller.Attribute prefixAttribute) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH));
        this.prefixAttribute = prefixAttribute;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(this.prefixAttribute)
                .addAttributes(Attribute.class)
                .addAttributes(ColumnAttribute.class)
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        Map<ColumnAttribute, Map.Entry<String, String>> columns = new EnumMap<>(ColumnAttribute.class);
        for (ColumnAttribute column : EnumSet.allOf(ColumnAttribute.class)) {
            ModelNode columnModel = column.resolveModelAttribute(context, model);
            String name = column.getColumnName().resolveModelAttribute(context, columnModel).asString();
            String type = column.getColumnType().resolveModelAttribute(context, columnModel).asString();
            columns.put(column, new AbstractMap.SimpleImmutableEntry<>(name, type));
        }

        int fetchSize = Attribute.FETCH_SIZE.resolveModelAttribute(context, model).asInt();
        String prefix = this.prefixAttribute.resolveModelAttribute(context, model).asString();
        boolean createOnStart = Attribute.CREATE_ON_START.resolveModelAttribute(context, model).asBoolean();
        boolean dropOnStop = Attribute.DROP_ON_STOP.resolveModelAttribute(context, model).asBoolean();

        Supplier<TableManipulationConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public TableManipulationConfiguration get() {
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
                        .create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
