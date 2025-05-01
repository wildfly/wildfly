/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.sql.DataSource;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.infinispan.persistence.jdbc.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the JDBC store of a cache.
 * @author Paul Ferraro
 */
public class JDBCStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<JdbcStringBasedStoreConfiguration, JdbcStringBasedStoreConfigurationBuilder> {

    static final CapabilityReferenceAttributeDefinition<DataSource> DATA_SOURCE = new CapabilityReferenceAttributeDefinition.Builder<>("data-source", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build()).setRequired(true).build();
    static final EnumAttributeDefinition<DatabaseType> DIALECT = new EnumAttributeDefinition.Builder<>("dialect", DatabaseType.class).setRequired(false).build();

    JDBCStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.JDBC;
            }

            @Override
            public ServiceDependency<JdbcStringBasedStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                PathAddress cacheAddress = context.getCurrentAddress().getParent();
                PathAddress containerAddress = cacheAddress.getParent();
                BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerAddress.getLastElement().getValue(), cacheAddress.getLastElement().getValue());
                ServiceDependency<ClassLoader> loader = configuration.getServiceDependency(CacheResourceDefinitionRegistrar.CLASS_LOADER);
                ServiceDependency<DataSource> dataSource = DATA_SOURCE.resolve(context, model);
                ServiceDependency<TableManipulationConfiguration> table = configuration.getServiceDependency(TableResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
                DatabaseType dialect = DIALECT.resolve(context, model);
                return new ServiceDependency<>() {
                    @Override
                    public void accept(RequirementServiceBuilder<?> builder) {
                        loader.accept(builder);
                        dataSource.accept(builder);
                        table.accept(builder);
                    }

                    @Override
                    public JdbcStringBasedStoreConfigurationBuilder get() {
                        JdbcStringBasedStoreConfigurationBuilder builder = new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class)
                                .segmented(true)
                                .transactional(false)
                                .dialect(dialect);
                        builder.connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class).setDataSource(dataSource.get());
                        builder.table().read(table.get());
                        ServiceLoader.load(TwoWayKey2StringMapper.class, loader.get()).findFirst().map(TwoWayKey2StringMapper::getClass).ifPresent(builder::key2StringMapper);
                        return builder;
                    }
                };
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(DATA_SOURCE, DIALECT))
                .requireChildResources(Set.of(TableResourceDefinitionRegistrar.REGISTRATION))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new TableResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }
}
