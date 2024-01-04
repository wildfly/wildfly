/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.management;

import jakarta.persistence.EntityManagerFactory;

import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.plugin.spi.PersistenceUnitService;

/**
 * EntityManagerFactoryLookup
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryLookup implements EntityManagerFactoryAccess {

    @Override
    public EntityManagerFactory entityManagerFactory(final String scopedPersistenceUnitName) {
        PersistenceUnitService persistenceUnitService = PersistenceUnitRegistryImpl.INSTANCE.getPersistenceUnitService(scopedPersistenceUnitName);
        if (persistenceUnitService == null) {
            return null;
        }
        return persistenceUnitService.getEntityManagerFactory();

    }

}
