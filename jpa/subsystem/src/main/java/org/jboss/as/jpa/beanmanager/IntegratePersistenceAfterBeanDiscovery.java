package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegratePersistenceAfterBeanDiscovery
 *
 * @author Scott Marlow
 */
public class IntegratePersistenceAfterBeanDiscovery implements Extension {

    private PersistenceUnitMetadata persistenceUnitMetadata;
    private TransactionManager transactionManager;
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        // PersistenceIntegrationWithCDI.addBeans();
        System.out.println("xxx IntegratePersistenceAfterBeanDiscovery afterBeanDiscovery called from " + Thread.currentThread().getName() + " with classloader = " + Thread.currentThread().getContextClassLoader().getName());
    }

    public void register(final PersistenceUnitMetadata persistenceUnitMetadata) {
        this.persistenceUnitMetadata = persistenceUnitMetadata;
    }

    /**
     * deploymentComplete() should be called when deployment is complete.
     */
    public void deploymentComplete() {
        persistenceUnitMetadata = null;
        transactionManager = null;
        transactionSynchronizationRegistry = null;
    }

}
