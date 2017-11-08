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
     * @param cacheType
     * @param wrapper
     */
    public static void stopCache(Classification cacheType, Wrapper wrapper) {
        for(EventListener eventListener: eventListeners) {
            eventListener.stopCache(cacheType, wrapper);
        }
    }


}
