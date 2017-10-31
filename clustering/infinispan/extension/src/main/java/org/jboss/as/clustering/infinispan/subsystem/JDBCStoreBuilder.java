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

import javax.sql.DataSource;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.DataSourceConnectionFactoryConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public abstract class JDBCStoreBuilder<C extends AbstractJdbcStoreConfiguration, B extends AbstractJdbcStoreConfigurationBuilder<C, B>> extends StoreBuilder<C, B> {

    private volatile ValueDependency<DataSource> dataSource;
    private volatile DatabaseType dialect;

    JDBCStoreBuilder(PathAddress cacheAddress, Class<B> builderClass) {
        super(cacheAddress, builderClass);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return this.dataSource.register(super.build(target));
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String dataSource = DATA_SOURCE.resolveModelAttribute(context, model).asString();
        this.dataSource = new InjectedValueDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, dataSource), DataSource.class);
        this.dialect = ModelNodes.optionalEnum(DIALECT.resolveModelAttribute(context, model), DatabaseType.class).orElse(null);
        return super.configure(context, model);
    }

    @Override
    public void accept(B builder) {
        builder.dialect(this.dialect).connectionFactory(DataSourceConnectionFactoryConfigurationBuilder.class).setDataSourceDependency(this.dataSource);
    }
}
