package org.jboss.as.jpa.beanmanager;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegratePersistenceAfterBeanDiscovery
 *
 * @author Scott Marlow
 */
public class IntegratePersistenceAfterBeanDiscovery implements Extension {

    private volatile PersistenceUnitMetadata persistenceUnitMetadata;
    private volatile TransactionManager transactionManager;
    private volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private volatile CompletableFuture<EntityManagerFactory> futureEntityManagerFactory;
    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        try {
            PersistenceIntegrationWithCDI.addBeans(event, persistenceUnitMetadata, transactionSynchronizationRegistry, transactionManager, futureEntityManagerFactory);
        } catch (RuntimeException e) {
            event.addDefinitionError(e);
        }
    }

    public void register(final PersistenceUnitMetadata persistenceUnitMetadata, TransactionManager transactionManager, TransactionSynchronizationRegistry transactionSynchronizationRegistry, CompletableFuture<EntityManagerFactory> futureEntityManagerFactory) {
        this.persistenceUnitMetadata = persistenceUnitMetadata;
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.futureEntityManagerFactory = futureEntityManagerFactory;
    }

    public CompletableFuture<EntityManagerFactory> getFutureEntityManagerFactory() {
        return futureEntityManagerFactory;
    }

    /**
     * deploymentComplete() should be called when deployment is complete.
     */
    public void deploymentComplete() {
        persistenceUnitMetadata = null;
        transactionManager = null;
        transactionSynchronizationRegistry = null;
        if (futureEntityManagerFactory != null) {
            // ensure other threads are released that are waiting for futureEntityManagerFactory to complete.
            futureEntityManagerFactory.complete(null);
        }
        futureEntityManagerFactory = null;
    }

}
