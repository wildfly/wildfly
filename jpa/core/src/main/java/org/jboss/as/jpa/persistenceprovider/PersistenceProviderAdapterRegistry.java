/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.persistenceprovider;

import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the registry of Persistence Provider adapters.
 *
 * Different versions of an adapter class, will use more specific (composite) key to reference the adapter that they want.
 * The composite key will be adapter module name + persistenceProviderClassName
 *
 * @author Scott Marlow
 */
public class PersistenceProviderAdapterRegistry {

    /**
     * Map from Persistence provider class name to the persistence provider adapter.
     */
    private static final ConcurrentHashMap<String, PersistenceProviderAdaptor>
        adapterRegistry = new ConcurrentHashMap<String, PersistenceProviderAdaptor>();

    public static PersistenceProviderAdaptor getPersistenceProviderAdaptor(String persistenceProviderClassName) {
        return adapterRegistry.get(persistenceProviderClassName);
    }

    public static PersistenceProviderAdaptor getPersistenceProviderAdaptor(String persistenceProviderClassName, String adapterModule) {
        return adapterRegistry.get(adapterModule+persistenceProviderClassName);
    }

    public static void putPersistenceProviderAdaptor(
        String persistenceProviderClassName,
        PersistenceProviderAdaptor persistenceProviderAdaptor) {
        adapterRegistry.putIfAbsent(persistenceProviderClassName, persistenceProviderAdaptor);
    }

    public static void putPersistenceProviderAdaptor(
        String persistenceProviderClassName,
        String adapterModule,
        PersistenceProviderAdaptor persistenceProviderAdaptor) {
        adapterRegistry.putIfAbsent(adapterModule+persistenceProviderClassName, persistenceProviderAdaptor);
    }

}
