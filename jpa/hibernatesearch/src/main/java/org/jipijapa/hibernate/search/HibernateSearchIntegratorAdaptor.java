/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.hibernate.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.jboss.jandex.Index;
import org.jipijapa.plugin.spi.PersistenceProviderIntegratorAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Implements the PersistenceProviderIntegratorAdaptor for Hibernate Search
 */
public class HibernateSearchIntegratorAdaptor implements PersistenceProviderIntegratorAdaptor {

    // Same as org.hibernate.search.engine.cfg.impl.OptionalPropertyContextImpl.MULTI_VALUE_SEPARATOR_PATTERN
    private static final Pattern MULTI_VALUE_SEPARATOR_PATTERN = Pattern.compile( "," );

    private Collection<Index> indexes;
    private final List<WildFlyHibernateOrmSearchMappingConfigurer> configurers = new ArrayList<>();

    @Override
    public void injectIndexes(Collection<Index> indexes) {
        this.indexes = indexes;
    }

    @Override
    public void addIntegratorProperties(Map<String, Object> properties, PersistenceUnitMetadata pu) {
        // See WildFlyHibernateOrmSearchMappingConfigurer
        addMappingConfigurer(properties, pu);
    }

    private void addMappingConfigurer(Map<String, Object> properties, PersistenceUnitMetadata pu) {
        Properties puProperties = pu.getProperties();

        List<Object> mappingConfigurerRefs = new ArrayList<>();
        Object customMappingConfigurerRefs = puProperties.get(HibernateOrmMapperSettings.MAPPING_CONFIGURER);
        if (customMappingConfigurerRefs != null) {
            // We need to parse user-provided config to preserve it
            try {
                mappingConfigurerRefs.addAll(ConvertUtils.convertMultiValue(MULTI_VALUE_SEPARATOR_PATTERN,
                        Function.identity(), customMappingConfigurerRefs));
            } catch (RuntimeException e) {
                throw JpaHibernateSearchLogger.JPA_HIBERNATE_SEARCH_LOGGER.failOnPropertyParsingForIntegration(
                        pu.getPersistenceUnitName(), HibernateOrmMapperSettings.MAPPING_CONFIGURER, e);
            }
        }
        // Change the default behavior of Hibernate Search regarding Jandex indexes
        WildFlyHibernateOrmSearchMappingConfigurer configurer = new WildFlyHibernateOrmSearchMappingConfigurer(indexes);
        configurers.add(configurer); // for cleaning up later, see methods below
        mappingConfigurerRefs.add(0, configurer);
        properties.put(HibernateOrmMapperSettings.MAPPING_CONFIGURER, mappingConfigurerRefs);
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        // Don't keep any reference to indexes, as those can be quite large.
        indexes = null;
        for (WildFlyHibernateOrmSearchMappingConfigurer configurer : configurers) {
            configurer.clearIndexReferences();
        }
        configurers.clear();
    }
}

