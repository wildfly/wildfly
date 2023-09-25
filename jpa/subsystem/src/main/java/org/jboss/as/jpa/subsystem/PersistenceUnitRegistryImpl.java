/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.subsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jipijapa.plugin.spi.PersistenceUnitServiceRegistry;


/**
 * Standard {@link PersistenceUnitServiceRegistry} implementation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PersistenceUnitRegistryImpl implements PersistenceUnitServiceRegistry {

    public static final PersistenceUnitRegistryImpl INSTANCE = new PersistenceUnitRegistryImpl();

    private final Map<String, PersistenceUnitService> registry = Collections.synchronizedMap(new HashMap<String, PersistenceUnitService>());


    @Override
    public PersistenceUnitService getPersistenceUnitService(String persistenceUnitResourceName) {
        return registry.get(persistenceUnitResourceName);
    }

    public void add(String scopedPersistenceUnitName, PersistenceUnitService service) {
        registry.put(scopedPersistenceUnitName, service);
    }

    public void remove(String scopedPersistenceUnitName) {
        registry.remove(scopedPersistenceUnitName);
    }
}
