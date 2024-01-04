/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.event.spi;

import java.util.Properties;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * lifecycle EventListener
 *
 * @author Scott Marlow
 */
public interface EventListener {

    /**
     * called before call to PersistenceProvider.createContainerEntityManagerFactory(PersistenceUnit, Map)
     * @param cacheType
     * @param persistenceUnitMetadata
     */
    void beforeEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata);

    /**
     * called after call to PersistenceProvider.createContainerEntityManagerFactory(PersistenceUnit, Map)
     * @param persistenceUnitMetadata
     */
    void afterEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata);

    /**
     * start cache
     *
     * @param cacheType
     * @param properties
     * @return an opaque cache wrapper that is later passed to stopCache
     */
    Wrapper startCache(Classification cacheType, Properties properties) throws Exception;

    /**
     * add dependencies on a cache
     *
     * @param cacheType
     * @param properties
     */
    void addCacheDependencies(Classification cacheType, Properties properties);

    /**
     * Stop cache
     *
     * @param cacheType
     * @param wrapper
     */
    void stopCache(Classification cacheType, Wrapper wrapper);
}
