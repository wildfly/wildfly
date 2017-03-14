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
import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class MixedKeyedJDBCStoreBuilder extends JDBCStoreBuilder<JdbcMixedStoreConfiguration, JdbcMixedStoreConfigurationBuilder> {

    private final InjectedValue<TableManipulationConfiguration> binaryTable = new InjectedValue<>();
    private final InjectedValue<TableManipulationConfiguration> stringTable = new InjectedValue<>();

    private final PathAddress cacheAddress;

    MixedKeyedJDBCStoreBuilder(PathAddress cacheAddress) {
        super(JdbcMixedStoreConfigurationBuilder.class, cacheAddress);
        this.cacheAddress = cacheAddress;
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(CacheComponent.BINARY_TABLE.getServiceName(this.cacheAddress), TableManipulationConfiguration.class, this.binaryTable)
                .addDependency(CacheComponent.STRING_TABLE.getServiceName(this.cacheAddress), TableManipulationConfiguration.class, this.stringTable)
        ;
    }

    @Override
    public void accept(JdbcMixedStoreConfigurationBuilder builder) {
        builder.binaryTable().read(this.binaryTable.getValue());
        builder.stringTable().read(this.stringTable.getValue());
        super.accept(builder);
    }
}
