/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DATA_SOURCE;
import static org.jboss.as.clustering.infinispan.subsystem.JDBCStoreResourceDefinition.Attribute.DIALECT;

import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class JDBCStoreBuilder extends StoreBuilder<JdbcStringBasedStoreConfiguration, JdbcStringBasedStoreConfigurationBuilder> {

    private final ValueDependency<TableManipulationConfiguration> table;
    private volatile ValueDependency<Module> module;
    private volatile ValueDependency<DataSource> dataSource;
    private volatile DatabaseType dialect;

    JDBCStoreBuilder(PathAddress address) {
        super(address, JdbcStringBasedStoreConfigurationBuilder.class);
        PathAddress cacheAddress = address.getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        this.table = new InjectedValueDependency<>(CacheComponent.STRING_TABLE.getServiceName(cacheAddress), TableManipulationConfiguration.class);
        this.module = new InjectedValueDependency<>(CacheContainerComponent.MODULE.getServiceName(containerAddress), Module.class);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        ServiceBuilder<PersistenceConfiguration> builder = super.build(target);
        return new CompositeDependency(this.table, this.module, this.dataSource).register(builder);
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String dataSource = DATA_SOURCE.resolveModelAttribute(context, model).asString();
        this.dataSource = new InjectedValueDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, dataSource), DataSource.class);
        this.dialect = ModelNodes.optionalEnum(DIALECT.resolveModelAttribute(context, model), DatabaseType.class).orElse(null);
        return super.configure(context, model);
    }

    @Override
    public void accept(JdbcStringBasedStoreConfigurationBuilder builder) {
        builder.table().read(this.table.getValue());
        for (TwoWayKey2StringMapper mapper : ServiceLoader.load(TwoWayKey2StringMapper.class, this.module.getValue().getClassLoader())) {
            builder.key2StringMapper(mapper.getClass());
            break;
        }
        builder.dialect(this.dialect).connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class).setDataSourceDependency(this.dataSource);
    }
}
