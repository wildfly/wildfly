/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.hibernate.search.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.jboss.jandex.Index;
import org.jipijapa.plugin.spi.PersistenceProviderIntegratorAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Additional {@link PersistenceProviderIntegratorAdaptor} shipped only by WildFly Preview.
 * <p>
 * WildFly Preview provisions the Apache HttpClient 5 based "rest5" Elasticsearch client and
 * makes it the default for every Elasticsearch backend.This adaptor selects the rest5 client
 * unless the application already chose a client explicitly.
 */
public class HibernateSearchElasticsearchRest5IntegratorAdaptor implements PersistenceProviderIntegratorAdaptor {

    // Value of the Hibernate Search backend "type" property (org.hibernate.search.engine.cfg.BackendSettings.TYPE)
    // selecting the Elasticsearch backend.
    private static final String ELASTICSEARCH_BACKEND_TYPE = "elasticsearch";
    // Namespace prefix for named backends: hibernate.search.backends.<name>.* (the default backend uses the
    // singular hibernate.search.backend.*). EngineSettings.BACKENDS is "hibernate.search.backends" (no trailing dot).
    private static final String NAMED_BACKENDS_PREFIX = EngineSettings.BACKENDS + ".";
    // Backend property radical selecting the pluggable Elasticsearch REST client;
    // see org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings#CLIENT_FACTORY.
    private static final String CLIENT_FACTORY = "client_factory";
    // The Apache HttpClient 5 based "rest5" Elasticsearch client, the only one WildFly Preview provisions.
    private static final String DEFAULT_ELASTICSEARCH_CLIENT_FACTORY = "elasticsearch-rest5";

    @Override
    public void injectIndexes(Collection<Index> indexes) {
        // Nothing to do: index handling is owned by the core Hibernate Search integrator adaptor.
    }

    @Override
    public void addIntegratorProperties(Map<String, Object> properties, PersistenceUnitMetadata pu) {
        // Default every Elasticsearch backend to the rest5 client provisioned by WildFly Preview.
        Properties puProperties = pu.getProperties();
        for (String backendName : elasticsearchBackendNames(puProperties)) {
            String clientFactoryKey = BackendSettings.backendKey(backendName, CLIENT_FACTORY);
            // Respect an explicit application choice, whether set in the persistence unit or by another integrator.
            if (!puProperties.containsKey(clientFactoryKey) && !properties.containsKey(clientFactoryKey)) {
                properties.put(clientFactoryKey, DEFAULT_ELASTICSEARCH_CLIENT_FACTORY);
            }
        }
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        // Nothing to clean up.
    }

    /**
     * @return the names of all Elasticsearch backends configured in the persistence unit, using {@code null} for the
     * default (unnamed) backend.
     */
    private static List<String> elasticsearchBackendNames(Properties puProperties) {
        List<String> backendNames = new ArrayList<>();
        // Default backend: hibernate.search.backend.type
        if (isElasticsearch(puProperties.getProperty(BackendSettings.backendKey(BackendSettings.TYPE)))) {
            backendNames.add(null);
        }
        // Named backends: hibernate.search.backends.<name>.type
        String typeSuffix = "." + BackendSettings.TYPE;
        for (String key : puProperties.stringPropertyNames()) {
            if (key.startsWith(NAMED_BACKENDS_PREFIX) && key.endsWith(typeSuffix)) {
                String backendName = key.substring(NAMED_BACKENDS_PREFIX.length(), key.length() - typeSuffix.length());
                // A backend name never contains a dot; this guards against deeper keys such as index-level properties.
                if (!backendName.isEmpty() && backendName.indexOf('.') < 0 && isElasticsearch(puProperties.getProperty(key))) {
                    backendNames.add(backendName);
                }
            }
        }
        return backendNames;
    }

    private static boolean isElasticsearch(String backendType) {
        return backendType != null && ELASTICSEARCH_BACKEND_TYPE.equals(backendType.trim());
    }
}
