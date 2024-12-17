/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import javax.sql.DataSource;

import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfigurationBuilder;

/**
 * Builds a {@link DataSourceConnectionFactoryConfiguration}.
 * @author Paul Ferraro
 */
public class DataSourceConnectionFactoryConfigurationBuilder<S extends AbstractJdbcStoreConfigurationBuilder<?, S>> extends AbstractJdbcStoreConfigurationChildBuilder<S> implements ConnectionFactoryConfigurationBuilder<DataSourceConnectionFactoryConfiguration> {

    private volatile DataSource dataSource;

    public DataSourceConnectionFactoryConfigurationBuilder(AbstractJdbcStoreConfigurationBuilder<?, S> builder) {
        super(builder);
    }

    public DataSourceConnectionFactoryConfigurationBuilder<S> setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
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
        return new DataSourceConnectionFactoryConfiguration(this.dataSource);
    }

    @Override
    public DataSourceConnectionFactoryConfigurationBuilder<S> read(DataSourceConnectionFactoryConfiguration template, Combine combine) {
        this.dataSource = template.getDataSource();
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return AttributeSet.EMPTY;
    }
}
