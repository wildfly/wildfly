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

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a service providing a {@link JdbcBinaryStoreConfiguration}.
 * @author Paul Ferraro
 */
public class BinaryKeyedJDBCStoreBuilder extends JDBCStoreBuilder<JdbcBinaryStoreConfiguration, JdbcBinaryStoreConfigurationBuilder> {

    private final ValueDependency<TableManipulationConfiguration> table;

    BinaryKeyedJDBCStoreBuilder(PathAddress cacheAddress) {
        super(cacheAddress, JdbcBinaryStoreConfigurationBuilder.class);
        this.table = new InjectedValueDependency<>(CacheComponent.BINARY_TABLE.getServiceName(cacheAddress), TableManipulationConfiguration.class);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return this.table.register(super.build(target));
    }

    @Override
    public void accept(JdbcBinaryStoreConfigurationBuilder builder) {
        builder.table().read(this.table.getValue());
        super.accept(builder);
    }
}
