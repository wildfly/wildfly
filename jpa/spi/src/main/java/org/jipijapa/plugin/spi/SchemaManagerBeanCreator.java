/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.util.List;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;

/**
 * SchemaManagerBeanCreator is for use with Persistence 3.2+ to create a CDI bean that injects the jakarta.persistence.SchemaManager.
 *
 * @author Scott Marlow
 */
public interface SchemaManagerBeanCreator {
    default void schemaManager(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitMetadata persistenceUnitMetadata, List<String> qualifiers, IntegrationWithCDIBag integrationWithCDIBag) throws ClassNotFoundException {
    }

    static SchemaManagerBeanCreator getImplementation(Class schemaManagerCreatorClass) {
        if (schemaManagerCreatorClass == null) {
            return new SchemaManagerBeanCreator() {
            };
        }
        try {
            return (SchemaManagerBeanCreator) schemaManagerCreatorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            // ignore exception on return no-op implementation
            // return a no-op implementation
            return new SchemaManagerBeanCreator() {
            };
        }
    }
}
