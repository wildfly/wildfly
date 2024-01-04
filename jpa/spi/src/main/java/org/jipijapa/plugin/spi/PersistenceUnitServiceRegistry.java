/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

/**
 * Registry of started persistence unit services.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface PersistenceUnitServiceRegistry {

    /**
     * Get the persistence unit service associated with the given management resource name.
     *
     * @param persistenceUnitResourceName the name of the management resource representing persistence unit
     *
     * @return a PersistenceUnitService or {@code null} if the persistence unit service has not been started or has been stopped
     */
    PersistenceUnitService getPersistenceUnitService(String persistenceUnitResourceName);


}
