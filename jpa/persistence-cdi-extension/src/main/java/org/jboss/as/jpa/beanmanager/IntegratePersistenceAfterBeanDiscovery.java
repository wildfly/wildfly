/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jipijapa.plugin.spi.IntegrationWithCDIBag;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * IntegratePersistenceAfterBeanDiscovery will setup Persistence/CDI integration as mentioned in jakarta.ee/specifications/platform/11/jakarta-platform-spec-11.0#a441
 * <p>
 * Obtaining an Entity Manager using CDI:
 * A Jakarta EE container must feature built-integration of Jakarta Persistence with the CDI bean manager, allowing injection of a container-managed
 * entity manager factory using the annotation jakarta.inject.Inject.
 * <p>
 * For each persistence unit, the container must make available a bean with:
 * - bean type EntityManager,
 * - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 * - the scope specified by the scope XML element in persistence.xml, or jakarta.transaction.TransactionScoped if no scope is explicitly specified,
 * - no interceptor bindings,
 * - a bean implementation which satisfies the requirements of this specification for a container-managed entity manager.
 * <p>
 * "EntityManager must also be available via the additional access methods specified at the beginning of this section."
 * <p>
 * Injecting an Entity Manager Factory using CDI:
 * A Jakarta EE container must feature built-in integration of Jakarta Persistence with the CDI bean manager,
 * allowing injection of a container-managed entity manager using the annotation jakarta.inject.Inject.
 * <p>
 * For each persistence unit, the container must make available a bean with:
 * - bean type EntityManagerFactory,
 * - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 * - scope jakarta.enterprise.context.ApplicationScoped,
 * - bean name given by the name of the persistence unit,
 * - no interceptor bindings,
 * - a bean implementation which satisfies the requirements of the Persistence specification for a container-managed entity manager factory.
 * <p>
 * Furthermore, the container must make available five beans with:
 * - bean types CriteriaBuilder, PersistenceUnitUtil, Cache, SchemaManager, and Metamodel, respectively,
 * - the qualifiers specified by qualifier XML elements in persistence.xml, or jakarta.enterprise.inject.Default, if no qualifiers are explicitly specified,
 * - scope jakarta.enterprise.context.Dependent,
 * - no interceptor bindings,
 * - a bean implementation which simply obtains the instance of the bean type by calling the appropriate getter method of the EntityManagerFactory bean.
 * - To access these bean types (CriteriaBuilder, PersistenceUnitUtil, Cache, SchemaManager, and Metamodel) from callsites that use @Resource or JNDI lookup,
 * users must first obtain the EntityManagerFactory and then use the appropriate getter methods.
 *
 * @author Scott Marlow
 */

public class IntegratePersistenceAfterBeanDiscovery implements PersistenceCdiExtension {
    private final CopyOnWriteArrayList<IntegrationWithCDIBagImpl> persistenceUnitIntegrationStuff = new CopyOnWriteArrayList<>();
    private volatile boolean afterBeanDiscoveryEventRanAlready = false;

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        afterBeanDiscoveryEventRanAlready = true;
        try {
            addBeans(event);
        } catch (RuntimeException e) {
            event.addDefinitionError(e);
        } finally {
            persistenceUnitIntegrationStuff.clear();
        }
    }

    @Override
    public IntegrationWithCDIBagImpl register(final PersistenceUnitMetadata persistenceUnitMetadata) {

        if (afterBeanDiscoveryEventRanAlready) {
            // this should never happen but still check just in case
            throw JpaLogger.ROOT_LOGGER.afterBeanDiscoveryEventRanAlready(persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
        final IntegrationWithCDIBagImpl integrationWithCDIBag = new IntegrationWithCDIBagImpl();
        integrationWithCDIBag.setPersistenceUnitMetadata(persistenceUnitMetadata);
        persistenceUnitIntegrationStuff.add(integrationWithCDIBag);
        return integrationWithCDIBag;
    }

    private static final List<String> defaultQualifier = List.of("jakarta.enterprise.inject.Default");
    private static final String transactionScoped = "jakarta.transaction.TransactionScoped";
    private static final String applicationScoped = "jakarta.enterprise.context.ApplicationScoped";

    public void addBeans(AfterBeanDiscovery afterBeanDiscovery) {
        boolean onePersistenceUnit = persistenceUnitIntegrationStuff.size() == 1;
        for (IntegrationWithCDIBagImpl integrationWithCDIBag : persistenceUnitIntegrationStuff) {
            PersistenceUnitMetadata persistenceUnitMetadata = integrationWithCDIBag.getPersistenceUnitMetadata();

            // determine the qualifiers to use for creating each bean
            List<String> qualifiers = Collections.emptyList();
            if (persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0) {
                qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
            } else {
                // mark the only default persistence unit as default.
                // With multiple persistence units, mark the one with hint "wildfly.jpa.default-unit" set to true.
                if (onePersistenceUnit || Configuration.isDefaultPersistenceUnit(persistenceUnitMetadata)) {
                    qualifiers = defaultQualifier;
                }
            }


            entityManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            entityManagerFactory(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            criteriaBuilder(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            persistenceUnitUtil(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            cache(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            metamodel(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            schemaManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);

        }
    }

    private void entityManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        String scope = persistenceUnitMetadata.getScopeAnnotationName();
        if (scope == null || scope.isEmpty()) {
            scope = transactionScoped;
        }

        // setup
        BeanConfigurator<EntityManager> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(EntityManager.class);


        Class<? extends Annotation> scopeAnnotation = null;
        try {
            scopeAnnotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
        } catch (ClassNotFoundException e) {
            throw JpaLogger.ROOT_LOGGER.classNotFound(e, "EntityManager", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
        beanConfigurator.scope(scopeAnnotation);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "EntityManager", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(EntityManager.class);
        beanConfigurator.produceWith(c -> {
                    return new TransactionScopedEntityManager(
                            persistenceUnitMetadata.getScopedPersistenceUnitName(),
                            new HashMap<>(),
                            integrationWithCDIBag.getEntityManagerFactory(),
                            SynchronizationType.SYNCHRONIZED,
                            integrationWithCDIBag.getTransactionSynchronizationRegistry(),
                            integrationWithCDIBag.getTransactionManager());
                }
        );
    }

    private void entityManagerFactory(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        // EntityManagerFactory setup
        BeanConfigurator<EntityManagerFactory> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(EntityManagerFactory.class);

        // Ensure that we do not get "WELD-001414: Bean name is ambiguous" deployment failure with duplicate persistence units in a deployment.
        if (!persistenceUnitMetadata.isDuplicate()) {
            beanConfigurator.name(persistenceUnitMetadata.getPersistenceUnitName());
        } else {
            JpaLogger.ROOT_LOGGER.willNotNameEntityManagerFactoryBean(persistenceUnitMetadata.getScopedPersistenceUnitName(), persistenceUnitMetadata.getPersistenceUnitName());
        }
        beanConfigurator.scope(ApplicationScoped.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "EntityManagerFactory", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(EntityManagerFactory.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory();
                }
        );
    }

    private void criteriaBuilder(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        BeanConfigurator<CriteriaBuilder> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(CriteriaBuilder.class);

        beanConfigurator.scope(Dependent.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "CriteriaBuilder", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            // beanConfigurator.addQualifier(qualifierType);
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }

        beanConfigurator.beanClass(CriteriaBuilder.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getCriteriaBuilder();
                }
        );

    }

    private void persistenceUnitUtil(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        BeanConfigurator<PersistenceUnitUtil> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(PersistenceUnitUtil.class);


        beanConfigurator.scope(Dependent.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "PersistenceUnitUtil", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(PersistenceUnitUtil.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getPersistenceUnitUtil();
                }
        );

    }

    private void cache(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        BeanConfigurator<Cache> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(Cache.class);

        beanConfigurator.scope(Dependent.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "Cache", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(Cache.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getCache();
                }
        );

    }


    private void metamodel(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        BeanConfigurator<Metamodel> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(Metamodel.class);

        beanConfigurator.scope(Dependent.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "Metamodel", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(Metamodel.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getMetamodel();
                }
        );
    }

    private void schemaManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBagImpl integrationWithCDIBag) {
        BeanConfigurator<SchemaManager> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(SchemaManager.class);

        beanConfigurator.scope(Dependent.class);

        for (String qualifier : qualifiers) {
            final Class<? extends Annotation> qualifierType;
            try {
                qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
            } catch (ClassNotFoundException e) {
                throw JpaLogger.ROOT_LOGGER.classNotFound(e, "SchemaManager", persistenceUnitMetadata.getPersistenceUnitName(), persistenceUnitMetadata.getScopedPersistenceUnitName());
            }
            // beanConfigurator.addQualifier(qualifierType);
            beanConfigurator.addQualifier(IntegratePersistenceAfterBeanDiscovery.ScopeProxy.createProxy(qualifierType));
        }
        beanConfigurator.beanClass(SchemaManager.class);
        beanConfigurator.produceWith(c -> {
                    return integrationWithCDIBag.getEntityManagerFactory().getSchemaManager();
                }
        );
    }

    protected record ScopeProxy(Class<? extends Annotation> annotationType) implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getName().equals("annotationType")) {
                return annotationType;
            }
            if (method.getName().equals("equals") && args != null && args.length == 1) {
                return annotationType.getName().equals(args[0].getClass().getName());
            }
            if (method.getName().equals("hashCode") && (args == null || args.length == 0)) {
                return annotationType.hashCode();
            }
            // This should likely not be used, but we'll se it just in case
            return method.getDefaultValue();
        }

        @SuppressWarnings("unchecked")
        static <T extends Annotation> T createProxy(final Class<T> annotationType) {
            return (T) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, new ScopeProxy(annotationType));
        }
    }


}
