/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegratePersistenceAfterBeanDiscovery
 *
 * @author Scott Marlow
 */
public class IntegratePersistenceAfterBeanDiscovery implements Extension {

    private volatile PersistenceUnitMetadata persistenceUnitMetadata;
    private final IntegrationWithCDIBagImpl integrationWithCDIBag = new IntegrationWithCDIBagImpl();
    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        try {
            new PersistenceIntegrationWithCDI().addBeans(event, persistenceUnitMetadata, integrationWithCDIBag);
        } catch (RuntimeException e) {
            event.addDefinitionError(e);
        }
    }

    public void register(final PersistenceUnitMetadata persistenceUnitMetadata) {
        this.persistenceUnitMetadata = persistenceUnitMetadata;
    }

    public IntegrationWithCDIBagImpl getIntegrationWithCDIBag() {
        return integrationWithCDIBag;
    }

}
