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

package org.jboss.as.jpa.subsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

/**
 * Standard {@link PersistenceUnitServiceRegistry} implementation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PersistenceUnitRegistryImpl implements PersistenceUnitServiceRegistry {

    private final Map<String, PersistenceUnitService> registry = Collections.synchronizedMap(new HashMap<String, PersistenceUnitService>());

    public Injector<PersistenceUnitService> getInjector() {
        return new PersistenceUnitInjector();
    }

    @Override
    public PersistenceUnitService getPersistenceUnitService(String persistenceUnitResourceName) {
        return registry.get(persistenceUnitResourceName);
    }

    private class PersistenceUnitInjector implements Injector<PersistenceUnitService> {

        private String filteredPersistenceUnitName;

        @Override
        public void inject(PersistenceUnitService value) throws InjectionException {
            filteredPersistenceUnitName = value.getScopedPersistenceUnitName();
            registry.put(filteredPersistenceUnitName, value);
        }

        @Override
        public void uninject() {
            if (filteredPersistenceUnitName != null) {
                registry.remove(filteredPersistenceUnitName);
                filteredPersistenceUnitName = null;
            }
        }
    }
}
