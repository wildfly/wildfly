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

package org.jboss.as.clustering.infinispan;

import java.util.function.Supplier;

import javax.sql.DataSource;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfigurationBuilder;
import org.wildfly.clustering.service.SimpleSupplierDependency;

/**
 * Builds a {@link DataSourceConnectionFactoryConfiguration}.
 * @author Paul Ferraro
 */
public class DataSourceConnectionFactoryConfigurationBuilder<S extends AbstractJdbcStoreConfigurationBuilder<?, S>> extends AbstractJdbcStoreConfigurationChildBuilder<S> implements ConnectionFactoryConfigurationBuilder<DataSourceConnectionFactoryConfiguration> {

    private volatile Supplier<DataSource> dependency;

    public DataSourceConnectionFactoryConfigurationBuilder(AbstractJdbcStoreConfigurationBuilder<?, S> builder) {
        super(builder);
    }

    public DataSourceConnectionFactoryConfigurationBuilder<S> setDataSourceDependency(Supplier<DataSource> dependency) {
        this.dependency = dependency;
        return this;
    }

    @Override
    public void validate() {
        // Nothing to validate
    }

    @Override
    public void validate(GlobalConfiguration globalConfig) {
        // Nothing to validate
    }

    @Override
    public DataSourceConnectionFactoryConfiguration create() {
        return new DataSourceConnectionFactoryConfiguration(this.dependency.get());
    }

    @Override
    public DataSourceConnectionFactoryConfigurationBuilder<S> read(DataSourceConnectionFactoryConfiguration template) {
        this.dependency = new SimpleSupplierDependency<>(template.getDataSource());
        return this;
    }
}
