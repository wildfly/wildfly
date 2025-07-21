/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * PersistenceIntegrationWithCDI will setup Persistence/CDI integration as mentioned in jakarta.ee/specifications/platform/11/jakarta-platform-spec-11.0#a441
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
public class PersistenceIntegrationWithCDI {

    private static final List defaultQualifier = List.of("jakarta.enterprise.inject.Default");
    private static final String transactionScoped = "jakarta.transaction.TransactionScoped";
    private static final String applicationScoped = "jakarta.enterprise.context.ApplicationScoped";
    private static final String dependentScoped = "jakarta.enterprise.context.Dependent";


    public static void addBeans(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitMetadata persistenceUnitMetadata, IntegrationWithCDIBag integrationWithCDIBag) {

        // determine the qualifiers to use for creating each bean
        List<String> qualifiers;
        if (persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0) {
            qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
        } else {
            qualifiers = defaultQualifier;
        }

        try {
            entityManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            entityManagerFactory(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            criteriaBuilder(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
        } catch (InstantiationException | IllegalAccessException e) {
            throw JpaLogger.ROOT_LOGGER.cannotAddBeans(e, persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
    }

    private static void entityManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) throws InstantiationException, IllegalAccessException {

        String scope = persistenceUnitMetadata.getScopeAnnotationName();
        if (scope == null || scope.isEmpty()) {
            scope = transactionScoped;
        }

        // EntityManager setup
        BeanConfigurator<EntityManager> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(EntityManager.class);

        try {

            Class<? extends Annotation> scopeAnnotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
            beanConfigurator.scope(scopeAnnotation);

            for (String qualifier : qualifiers) {
                final Class<? extends Annotation> qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
                beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
            }
            Class<?> entityManagerClass = EntityManager.class;
            beanConfigurator.beanClass(entityManagerClass);
            beanConfigurator.produceWith(c -> {
                        return new TransactionScopedEntityManager(persistenceUnitMetadata.getScopedPersistenceUnitName(), new HashMap<>(), integrationWithCDIBag.getEntityManagerFactory(), SynchronizationType.SYNCHRONIZED, integrationWithCDIBag.getTransactionSynchronizationRegistry(), integrationWithCDIBag.getTransactionManager());
                    }
            ).disposeWith((em, instance) -> em.close());  // TODO: validate that container-managed entity manager manages when close is called.
        } catch (ClassNotFoundException e) {
            throw JpaLogger.ROOT_LOGGER.classNotFound(e, persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
    }

    private static void entityManagerFactory(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) throws InstantiationException, IllegalAccessException {

        String scope = applicationScoped;

        // EntityManagerFactory setup
        BeanConfigurator<EntityManagerFactory> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(EntityManagerFactory.class);
        beanConfigurator.name(persistenceUnitMetadata.getPersistenceUnitName());

        try {

            Class<? extends Annotation> scopeAnnotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
            beanConfigurator.scope(scopeAnnotation);

            for (String qualifier : qualifiers) {
                final Class<? extends Annotation> qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
                // beanConfigurator.addQualifier(qualifierType);
                beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));

            }
            Class<?> entityManagerFactoryClass = EntityManagerFactory.class;
            beanConfigurator.beanClass(entityManagerFactoryClass);
            beanConfigurator.produceWith(c -> {
                        return integrationWithCDIBag.getEntityManagerFactory();
                    }
            );
        } catch (ClassNotFoundException e) {
            throw JpaLogger.ROOT_LOGGER.classNotFound(e, persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
    }

    private static void criteriaBuilder(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) throws InstantiationException, IllegalAccessException {

        String scope = dependentScoped;

        BeanConfigurator<CriteriaBuilder> beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addTransitiveTypeClosure(CriteriaBuilder.class);

        try {

            Class<? extends Annotation> scopeAnnotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
            beanConfigurator.scope(scopeAnnotation);

            for (String qualifier : qualifiers) {
                final Class<? extends Annotation> qualifierType = persistenceUnitMetadata.getClassLoader()
                        .loadClass(qualifier)
                        .asSubclass(Annotation.class);
                // beanConfigurator.addQualifier(qualifierType);
                beanConfigurator.addQualifier(ScopeProxy.createProxy(qualifierType));
            }
            Class<?> criteriaBuilderClass = CriteriaBuilder.class;
            beanConfigurator.beanClass(criteriaBuilderClass);
            beanConfigurator.produceWith(c -> {
                        return integrationWithCDIBag.getEntityManagerFactory().getCriteriaBuilder();
                    }
            );
        } catch (ClassNotFoundException e) {
            throw JpaLogger.ROOT_LOGGER.classNotFound(e, persistenceUnitMetadata.getScopedPersistenceUnitName());
        }
    }

    private record ScopeProxy(Class<? extends Annotation> annotationType) implements InvocationHandler {

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
                return (T) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[] {annotationType}, new ScopeProxy(annotationType));
            }
        }

}
