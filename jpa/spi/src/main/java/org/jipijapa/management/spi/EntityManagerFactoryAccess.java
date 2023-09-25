/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.management.spi;

import jakarta.persistence.EntityManagerFactory;

/**
 * EntityManagerFactoryAccess
 *
 * @author Scott Marlow
 */
public interface EntityManagerFactoryAccess {
    /**
     * returns the entity manager factory that statistics should be obtained for.
     *
     * @throws IllegalStateException if scopedPersistenceUnitName is not found
     *
     * @param scopedPersistenceUnitName is persistence unit name scoped to the current platform
     *
     * @return EntityManagerFactory
     */
    EntityManagerFactory entityManagerFactory(String scopedPersistenceUnitName) throws IllegalStateException;

}
