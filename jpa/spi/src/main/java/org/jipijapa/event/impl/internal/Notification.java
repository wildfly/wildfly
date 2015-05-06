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

package org.jipijapa.event.impl.internal;

import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.spi.EventListener;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Event Notification
 *
 * @author Scott Marlow
 */
public class Notification {
    private static final CopyOnWriteArrayList<EventListener> eventListeners = new CopyOnWriteArrayList<EventListener>();

    public static void add(EventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public static void remove(EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    /**
     * called before call to PersistenceProvider.createContainerEntityManagerFactory(PersistenceUnit, Map)
     * @param persistenceUnitMetadata
     */
    public static void beforeEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata) {
        for(EventListener eventListener: eventListeners) {
            eventListener.beforeEntityManagerFactoryCreate(cacheType, persistenceUnitMetadata);
        }
    }

    /**
     * called after call to PersistenceProvider.createContainerEntityManagerFactory(PersistenceUnit, Map)
     * @param persistenceUnitMetadata
     */
    public static void afterEntityManagerFactoryCreate(Classification cacheType, PersistenceUnitMetadata persistenceUnitMetadata) {
        for(EventListener eventListener: eventListeners) {
            eventListener.afterEntityManagerFactoryCreate(cacheType, persistenceUnitMetadata);
        }
    }

    /**
     * start cache
     *
     * @param cacheType
     * @param properties
     * @return an opaque cache wrapper that is later passed to stopCache
     */
    public static Wrapper startCache(Classification cacheType, Properties properties) throws Exception {
        Wrapper result = null;
        for(EventListener eventListener: eventListeners) {
            Wrapper value = eventListener.startCache(cacheType, properties);
            if (value != null && result == null) {
                result = value;     // return the first non-null wrapper value returned from a listener
            }
        }

        return result;
    }

    /**
     * add cache dependencies
     *
     * @param properties
     */
    public static void addCacheDependencies(Classification cacheType, Properties properties) {
        for(EventListener eventListener: eventListeners) {
            eventListener.addCacheDependencies(cacheType, properties);
        }
    }

    /**
     * Stop cache
     *
     * @param wrapper
     * @param skipStop will be true if the cache shouldn't be stopped
     */
    public static void stopCache(Classification cacheType, Wrapper wrapper, boolean skipStop) {
        for(EventListener eventListener: eventListeners) {
            eventListener.stopCache(cacheType, wrapper, skipStop);
        }
    }


}
