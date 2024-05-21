/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import java.util.function.Supplier;

import javax.sql.DataSource;

import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfigurationBuilder;
import org.wildfly.common.function.Functions;

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
    public DataSourceConnectionFactoryConfigurationBuilder<S> read(DataSourceConnectionFactoryConfiguration template, Combine combine) {
        this.dependency = Functions.constantSupplier(template.getDataSource());
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return AttributeSet.EMPTY;
    }
}
