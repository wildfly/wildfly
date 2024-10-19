/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.infinispan.persistence.jdbc.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.clustering.infinispan.persistence.jdbc.JDBCStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.jdbc.JDBCStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public enum JDBCStoreResourceDescription implements StoreResourceDescription<JDBCStoreConfiguration, JDBCStoreConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = PersistenceResourceDescription.pathElement("jdbc");

    static final CapabilityReferenceAttributeDefinition<DataSource> DATA_SOURCE = new CapabilityReferenceAttributeDefinition.Builder<>("data-source", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build()).setRequired(true).build();
    static final EnumAttributeDefinition<DatabaseType> DIALECT = new EnumAttributeDefinition.Builder<>("dialect", DatabaseType.class).setRequired(false).build();

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(DATA_SOURCE, DIALECT), StoreResourceDescription.super.getAttributes());
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return StoreResourceDescription.super.apply(builder)
                .requireChildResources(Set.of(TableResourceDescription.INSTANCE))
                ;
    }

    @Override
    public ServiceDependency<JDBCStoreConfigurationBuilder> resolveStore(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        ServiceDependency<GlobalConfiguration> global = ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION, context.getCurrentAddress().getParent().getParent().getLastElement().getValue());
        ServiceDependency<DataSource> dataSource = DATA_SOURCE.resolve(context, model);
        ServiceDependency<TableManipulationConfiguration> table = ServiceDependency.on(TableResourceDescription.INSTANCE.getServiceDescriptor(), containerAddress.getLastElement().getValue(), cacheAddress.getLastElement().getValue());
        DatabaseType dialect = DIALECT.resolve(context, model);
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                global.accept(builder);
                dataSource.accept(builder);
                table.accept(builder);
            }

            @Override
            public JDBCStoreConfigurationBuilder get() {
                JDBCStoreConfigurationBuilder builder = new ConfigurationBuilder().persistence().addStore(JDBCStoreConfigurationBuilder.class)
                        .segmented(true)
                        .transactional(false)
                        .dialect(dialect);
                builder.connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class).setDataSource(dataSource.get());
                builder.table().read(table.get());
                ServiceLoader.load(TwoWayKey2StringMapper.class, global.get().classLoader()).findFirst().map(TwoWayKey2StringMapper::getClass).ifPresent(builder::key2StringMapper);
                return builder;
            }
        };
    }
}
