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

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.as.controller.PathAddress;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class StringKeyedJDBCStoreBuilder extends JDBCStoreBuilder<JdbcStringBasedStoreConfiguration, JdbcStringBasedStoreConfigurationBuilder> {

    private final ValueDependency<TableManipulationConfiguration> table;
    private final ValueDependency<Module> module;

    StringKeyedJDBCStoreBuilder(PathAddress cacheAddress) {
        super(cacheAddress, JdbcStringBasedStoreConfigurationBuilder.class);
        PathAddress containerAddress = cacheAddress.getParent();
        this.table = new InjectedValueDependency<>(CacheComponent.STRING_TABLE.getServiceName(cacheAddress), TableManipulationConfiguration.class);
        this.module = new InjectedValueDependency<>(CacheContainerComponent.MODULE.getServiceName(containerAddress), Module.class);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        ServiceBuilder<PersistenceConfiguration> builder = super.build(target);
        Stream.of(this.table, this.module).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public void accept(JdbcStringBasedStoreConfigurationBuilder builder) {
        builder.table().read(this.table.getValue());
        StreamSupport.stream(ServiceLoader.load(TwoWayKey2StringMapper.class, this.module.getValue().getClassLoader()).spliterator(), false).findFirst()
                .ifPresent(impl -> builder.key2StringMapper(impl.getClass()));
        super.accept(builder);
    }
}
