/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.infinispan.persistence.jdbc.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JDBCStoreResourceDefinition extends StoreResourceDefinition<JdbcStringBasedStoreConfiguration, JdbcStringBasedStoreConfigurationBuilder> {

    static final PathElement PATH = pathElement("jdbc");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DATA_SOURCE("data-source", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true)
                        .setCapabilityReference(CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build())
                        ;
            }
        },
        DIALECT("dialect", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true)
                        .setRequired(false)
                        .setValidator(EnumValidator.create(DatabaseType.class))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES))
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(StringTableResourceDefinition.PATH);

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    .addRequiredChildren(REQUIRED_CHILDREN)
                    ;
        }
    }

    JDBCStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator(), JdbcStringBasedStoreConfigurationBuilder.class);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new StringTableResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public Map.Entry<Map.Entry<Supplier<JdbcStringBasedStoreConfigurationBuilder>, Consumer<JdbcStringBasedStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {

        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        String dataSourceName = Attribute.DATA_SOURCE.resolveModelAttribute(context, model).asString();
        DatabaseType dialect = Optional.ofNullable(Attribute.DIALECT.resolveModelAttribute(context, model).asStringOrNull()).map(DatabaseType::valueOf).orElse(null);

        ServiceDependency<DataSource> dataSource = ServiceDependency.on(CommonServiceDescriptor.DATA_SOURCE, dataSourceName);
        ServiceDependency<TableManipulationConfiguration> table = ServiceDependency.on(TableResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<List<Module>> modules = ServiceDependency.on(CacheResourceDefinition.CACHE_MODULES, containerName, cacheName);

        Map.Entry<Map.Entry<Supplier<JdbcStringBasedStoreConfigurationBuilder>, Consumer<JdbcStringBasedStoreConfigurationBuilder>>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = super.resolve(context, model);
        Supplier<JdbcStringBasedStoreConfigurationBuilder> builderFactory = entry.getKey().getKey();
        Consumer<JdbcStringBasedStoreConfigurationBuilder> configurator = entry.getKey().getValue().andThen(new Consumer<>() {
            @Override
            public void accept(JdbcStringBasedStoreConfigurationBuilder builder) {
                builder.table().read(table.get());
                TwoWayKey2StringMapper mapper = this.findMapper();
                if (mapper != null) {
                    builder.key2StringMapper(mapper.getClass());
                }
                builder.dialect(dialect).transactional(false);
                builder.connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class).setDataSourceDependency(dataSource);
            }

            private TwoWayKey2StringMapper findMapper() {
                for (Module module : modules.get()) {
                    for (TwoWayKey2StringMapper mapper : module.loadService(TwoWayKey2StringMapper.class)) {
                        return mapper;
                    }
                }
                return null;
            }
        });
        Stream<Consumer<RequirementServiceBuilder<?>>> dependencies = entry.getValue();

        return MapEntry.of(MapEntry.of(builderFactory, configurator), Stream.concat(dependencies, Stream.of(dataSource, table, modules)));
    }
}
