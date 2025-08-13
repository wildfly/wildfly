/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegratePersistenceAfterBeanDiscovery
 *
 * @author Scott Marlow
 */
public class IntegratePersistenceAfterBeanDiscovery implements Extension {
    private final CopyOnWriteArrayList<IntegrationWithCDIBagImpl> copyOnWriteArrayList = new CopyOnWriteArrayList();
    private volatile boolean afterBeanDiscoveryEventRanAlready = false;

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        afterBeanDiscoveryEventRanAlready = true;
        try {
            for (IntegrationWithCDIBagImpl integrationWithCDIBag: copyOnWriteArrayList) {
                new PersistenceIntegrationWithCDI().addBeans(event, integrationWithCDIBag.getPersistenceUnitMetadata(), integrationWithCDIBag);
            }
        } catch (RuntimeException e) {
            event.addDefinitionError(e);
        }
    }

    public IntegrationWithCDIBagImpl register(final PersistenceUnitMetadata persistenceUnitMetadata) {

        if (afterBeanDiscoveryEventRanAlready) {
            // this should never happen but still check just in case
            throw JpaLogger.ROOT_LOGGER.afterBeanDiscoveryEventRanAlready(persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
        final IntegrationWithCDIBagImpl integrationWithCDIBag = new IntegrationWithCDIBagImpl();
        integrationWithCDIBag.setPersistenceUnitMetadata(persistenceUnitMetadata);
        copyOnWriteArrayList.add(integrationWithCDIBag);
        return integrationWithCDIBag;
    }
}
