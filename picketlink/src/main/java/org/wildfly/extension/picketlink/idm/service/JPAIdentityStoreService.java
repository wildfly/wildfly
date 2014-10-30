/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.picketlink.idm.service;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;
import org.wildfly.extension.picketlink.idm.config.JPAStoreSubsystemConfiguration;
import org.wildfly.extension.picketlink.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.wildfly.extension.picketlink.idm.jpa.transaction.TransactionalEntityManagerHelper;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.isAbstract;
import static org.picketlink.common.util.StringUtil.isNullOrEmpty;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public class JPAIdentityStoreService implements Service<JPAIdentityStoreService> {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";
    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "identity";

    private final JPAStoreSubsystemConfigurationBuilder configurationBuilder;
    private JPAStoreSubsystemConfiguration storeConfig;
    private EntityManagerFactory emf;
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
    private InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
    private TransactionalEntityManagerHelper transactionalEntityManagerHelper;

    public JPAIdentityStoreService(JPAStoreSubsystemConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        this.storeConfig = this.configurationBuilder.create();
        this.transactionalEntityManagerHelper = new TransactionalEntityManagerHelper(
            this.transactionSynchronizationRegistry.getValue(),
            this.transactionManager.getValue());

        try {
            configureEntityManagerFactory();
            configureEntities();
        } catch (Exception e) {
            throw ROOT_LOGGER.idmJpaStartFailed(e);
        }

        this.configurationBuilder.addContextInitializer(new ContextInitializer() {
            @Override
            public void initContextForStore(IdentityContext context, IdentityStore<?> store) {
                if (store instanceof JPAIdentityStore) {
                    EntityManager entityManager = context.getParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER);

                    if (entityManager == null || !entityManager.isOpen()) {
                        context.setParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER, getEntityManager(getTransactionManager().getValue()));
                    }
                }
            }
        });
    }

    @Override
    public void stop(StopContext stopContext) {
        if (this.storeConfig.getEntityManagerFactoryJndiName() == null) {
            this.emf.close();
        }
    }

    @Override
    public JPAIdentityStoreService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<TransactionManager> getTransactionManager() {
        return this.transactionManager;
    }

    public InjectedValue<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistry() {
        return this.transactionSynchronizationRegistry;
    }

    private void configureEntityManagerFactory() {
        if (this.storeConfig.getEntityManagerFactoryJndiName() != null) {
            this.emf = lookupEntityManagerFactory();
        } else {
            this.emf = createEmbeddedEntityManagerFactory();
        }
    }

    private EntityManagerFactory lookupEntityManagerFactory() {
        ROOT_LOGGER.debugf("Looking up EntityManagerFactory from [%s]", this.storeConfig.getEntityManagerFactoryJndiName());
        try {
            return (EntityManagerFactory) new InitialContext().lookup(this.storeConfig.getEntityManagerFactoryJndiName());
        } catch (NamingException e) {
            throw ROOT_LOGGER.idmJpaEMFLookupFailed(this.storeConfig.getEntityManagerFactoryJndiName());
        }
    }

    private EntityManagerFactory createEmbeddedEntityManagerFactory() {
        ROOT_LOGGER.debugf("Creating embedded EntityManagerFactory.");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();
            String dataSourceJndiUrl = this.storeConfig.getDataSourceJndiUrl();

            if (!isNullOrEmpty(dataSourceJndiUrl)) {
                ROOT_LOGGER.debugf("Using datasource [%s] for embedded EntityManagerFactory.", dataSourceJndiUrl);
                properties.put("javax.persistence.jtaDataSource", dataSourceJndiUrl);
            }

            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform());

            Module entityModule = this.storeConfig.getEntityModule();

            if (entityModule != null) {
                Thread.currentThread().setContextClassLoader(entityModule.getClassLoader());
            }

            return Persistence.createEntityManagerFactory(this.storeConfig.getEntityModuleUnitName(), properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void configureEntities() {
        Set<EntityType<?>> mappedEntities = this.emf.getMetamodel().getEntities();

        for (EntityType<?> entity : mappedEntities) {
            Class<?> javaType = entity.getJavaType();
            if (!isAbstract(javaType.getModifiers()) && isIdentityEntity(javaType)) {
                ROOT_LOGGER.debugf("Mapping entity [%s] to JPA Identity Store.", javaType.getName());
                this.configurationBuilder.mappedEntity(javaType);
            }
        }
    }

    private boolean isIdentityEntity(Class<?> cls) {
        Class<?> checkClass = cls;

        while (!checkClass.equals(Object.class)) {
            for (Annotation a : checkClass.getAnnotations()) {
                if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                    return true;
                }
            }

            // No class annotation was found, check the fields
            for (Field f : checkClass.getDeclaredFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                        return true;
                    }
                }
            }

            // Check the superclass
            checkClass = checkClass.getSuperclass();
        }

        return false;
    }

    private EntityManager getEntityManager(TransactionManager transactionManager) {
        EntityManager entityManager = getOrCreateTransactionalEntityManager(transactionManager);

        if (entityManager == null || !entityManager.isOpen()) {
            entityManager = createEntityManager(transactionManager);
        }

        return entityManager;
    }

    /**
     * <p>Returns an {@link javax.persistence.EntityManager} associated with the actual {@link javax.transaction.Transaction}, if present.</p>
     *
     * <p>If {@link javax.transaction.Transaction} is {@link Status#STATUS_ACTIVE}, this method tries to return an entity manager
     * already associated with it. If there is no entity manager a new one is created.</p>
     *
     * <p>This method is specially useful when IDM is being called from an EJB or any other component that have already started
     * a transaction. In this case, the client code is responsible to commit or rollback the transaction accordingly.</p>
     *
     * <p>The returned {@link EntityManager} is always closed before the transaction completes.</p>
     *
     * @param transactionManager The transaction manager.
     *
     * @return The {@link EntityManager} associated with the actual transaction or null if there is no active transactions.
     */
    private EntityManager getOrCreateTransactionalEntityManager(TransactionManager transactionManager) {
        try {
            if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                EntityManager entityManager = this.transactionalEntityManagerHelper.getTransactionScopedEntityManager(getPersistenceUnitName());

                if (entityManager == null) {
                    entityManager = createEntityManager(transactionManager);
                    this.transactionalEntityManagerHelper.putEntityManagerInTransactionRegistry(getPersistenceUnitName(), entityManager);
                }

                return entityManager;
            }
        } catch (Exception e) {
            throw ROOT_LOGGER.idmJpaFailedCreateTransactionEntityManager(e);
        }

        return null;
    }

    private String getPersistenceUnitName() {
        String persistenceUnitName = this.storeConfig.getEntityModuleUnitName();

        if (persistenceUnitName == null) {
            persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
        }

        return persistenceUnitName;
    }

    private EntityManager createEntityManager(TransactionManager transactionManager) {
        return (EntityManager) Proxy.newProxyInstance(Thread.currentThread()
            .getContextClassLoader(), new Class<?>[]{EntityManager.class}, new EntityManagerInvocationHandler(this.emf.createEntityManager(),
            this.storeConfig.getEntityModule(), transactionManager));
    }

    private class EntityManagerInvocationHandler implements InvocationHandler {

        private final Module entityModule;
        private final TransactionManager transactionManager;
        private final EntityManager entityManager;

        public EntityManagerInvocationHandler(EntityManager entityManager, Module entitiesModule, TransactionManager transactionManager) {
            this.entityManager = entityManager;
            this.entityModule = entitiesModule;
            this.transactionManager = transactionManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean isManagedTransaction = false;

            if (isTxRequired(method)) {
                if (this.transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION) {
                    this.transactionManager.begin();
                    isManagedTransaction = true;
                }

                this.entityManager.joinTransaction();
            }

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                if (this.entityModule != null) {
                    Thread.currentThread().setContextClassLoader(this.entityModule.getClassLoader());
                }

                return method.invoke(this.entityManager, args);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);

                if (isManagedTransaction) {
                    if (this.transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                        this.transactionManager.commit();
                        this.transactionManager.suspend();
                    }
                }
            }
        }

        private boolean isTxRequired(Method method) {
            String n = method.getName();
            return "flush".equals(n) || "getLockMode".equals(n) || "lock".equals(n) || "merge".equals(n) || "persist".equals(n) || "refresh".equals(n) || "remove".equals(n);
        }
    }
}
