/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param skipStop will be true if the cache shouldn't be stopped
     */
    void stopCache(Classification cacheType, Wrapper wrapper, boolean skipStop);
}
