/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import jakarta.persistence.EntityManagerFactory;

/**
 * Persistence unit service
 *
 * @author Scott Marlow
 */
public interface PersistenceUnitService {
    /**
     * get the entity manager factory that represents the persistence unit service.  This corresponds to a
     * persistence unit definition in a persistence.xml
     *
     * @return EntityManagerFactory or {@code null} if this service has not been started or has been stopped
     */
    EntityManagerFactory getEntityManagerFactory();

    /**
     * Gets the scoped name of this persistence unit.
     *
     * @return the name
     */
    String getScopedPersistenceUnitName();
}
