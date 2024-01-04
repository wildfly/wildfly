/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import static org.infinispan.persistence.jdbc.common.configuration.Attribute.KEY_TO_STRING_MAPPER;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfiguration;
import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;

/**
 * Configuration of a {@link JDBCStore}.
 * @author Paul Ferraro
 */
@BuiltBy(JDBCStoreConfigurationBuilder.class)
@ConfigurationFor(JDBCStore.class)
public class JDBCStoreConfiguration extends JdbcStringBasedStoreConfiguration {
    // JdbcStringBasedStoreConfiguration.KEY2STRING_MAPPER is not accessible
    public static final AttributeDefinition<String> KEY2STRING_MAPPER = JdbcStringBasedStoreConfiguration.attributeDefinitionSet().<String>attribute(KEY_TO_STRING_MAPPER).getAttributeDefinition();

    public static AttributeSet attributeDefinitionSet() {
       return new AttributeSet(JDBCStoreConfiguration.class, AbstractJdbcStoreConfiguration.attributeDefinitionSet(), KEY2STRING_MAPPER);
    }

    public JDBCStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async, ConnectionFactoryConfiguration connectionFactory, TableManipulationConfiguration table) {
        super(attributes, async, connectionFactory, table);
    }
}
