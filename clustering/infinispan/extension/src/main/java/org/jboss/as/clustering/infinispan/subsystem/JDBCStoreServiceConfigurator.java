/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DATA_SOURCE;
import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DIALECT;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.infinispan.persistence.jdbc.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class JDBCStoreServiceConfigurator extends StoreServiceConfigurator<JdbcStringBasedStoreConfiguration, JdbcStringBasedStoreConfigurationBuilder> {

    private final SupplierDependency<TableManipulationConfiguration> table;
    private volatile SupplierDependency<List<Module>> modules;
    private volatile SupplierDependency<DataSource> dataSource;
    private volatile DatabaseType dialect;

    JDBCStoreServiceConfigurator(PathAddress address) {
        super(address, JdbcStringBasedStoreConfigurationBuilder.class);
        PathAddress cacheAddress = address.getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        this.table = new ServiceSupplierDependency<>(CacheComponent.STRING_TABLE.getServiceName(cacheAddress));
        this.modules = new ServiceSupplierDependency<>(CacheContainerComponent.MODULES.getServiceName(containerAddress));
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(new CompositeDependency(this.table, this.modules, this.dataSource).register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String dataSource = DATA_SOURCE.resolveModelAttribute(context, model).asString();
        this.dataSource = new ServiceSupplierDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, dataSource));
        this.dialect = Optional.ofNullable(DIALECT.resolveModelAttribute(context, model).asStringOrNull()).map(DatabaseType::valueOf).orElse(null);
        return super.configure(context, model);
    }

    @Override
    public void accept(JdbcStringBasedStoreConfigurationBuilder builder) {
        builder.table().read(this.table.get());
        TwoWayKey2StringMapper mapper = this.findMapper();
        if (mapper != null) {
            builder.key2StringMapper(mapper.getClass());
        }
        builder.segmented(true)
                .transactional(false)
                .dialect(this.dialect)
                .connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class)
                .setDataSourceDependency(this.dataSource);
    }

    private TwoWayKey2StringMapper findMapper() {
        for (Module module : this.modules.get()) {
            for (TwoWayKey2StringMapper mapper : module.loadService(TwoWayKey2StringMapper.class)) {
                return mapper;
            }
        }
        return null;
    }
}
