/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import org.jboss.jandex.Index;

import java.util.Collection;
import java.util.Map;

/**
 * Adaptor for integrators into persistence providers, e.g. Hibernate Search.
 */
public interface PersistenceProviderIntegratorAdaptor {

    /**
     * @param indexes The index views for the unit being deployed
     */
    void injectIndexes(Collection<Index> indexes);

    /**
     * Adds any integrator-specific persistence unit properties
     *
     * @param properties
     * @param pu
     */
    void addIntegratorProperties(Map<String, Object> properties, PersistenceUnitMetadata pu);

    /**
     * Called right after persistence provider is invoked to create container entity manager factory.
     */
    void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu);

}

