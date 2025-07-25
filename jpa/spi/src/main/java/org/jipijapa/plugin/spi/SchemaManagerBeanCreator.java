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
    default void schemaManager(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitMetadata persistenceUnitMetadata, List<String> qualifiers, IntegrationWithCDIBag integrationWithCDIBag) throws IllegalAccessException {
    }

    static SchemaManagerBeanCreator instance = null;

    static SchemaManagerBeanCreator getImplementation(ClassLoader persistenceSubSystemClassLoader) {
        if (instance == null) {
            synchronized (SchemaManagerBeanCreator.class) {
                if (instance == null) {
                    String schemaManagerBeanCreatorImpl = "org.jboss.as.jpa.beanmanager.Persistence32";
                    try {
                        return (SchemaManagerBeanCreator) persistenceSubSystemClassLoader.loadClass(schemaManagerBeanCreatorImpl).newInstance();
                    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ignore) {
                        // ignore exception on return no-op implementation
                    }

                    // return a no-op implementation
                    return new SchemaManagerBeanCreator() {
                    };
                }
            }
        }
        return instance;
    }

}
