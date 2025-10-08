/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
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
    private static final Set<String> DEFAULT_QUALIFIERS = Set.of(Default.class.getName(), Any.class.getName());
    private final CopyOnWriteArrayList<IntegrationWithCDIBagImpl> persistenceUnitIntegrationStuff = new CopyOnWriteArrayList<>();
    // Captures a map of produces for the known persistence injection types
    private final Map<Class<?>, Collection<Set<String>>> foundProducers = new HashMap<>();
    private volatile boolean afterBeanDiscoveryEventRanAlready = false;

    /**
     * Process producer fields for known persistence injection types
     *
     * @param producerField the producer field
     */
    public void processProducerFields(@Observes final ProcessProducerField<?, ?> producerField) {
        processBeanProducer(producerField.getBean());
    }

    /**
     * Process producer methods for the known persistence injection types
     *
     * @param processProducerMethod the producer method
     */
    public void processProducerMethods(@Observes final ProcessProducerMethod<?, ?> processProducerMethod) {
        processBeanProducer(processProducerMethod.getBean());
    }

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

    private static final String transactionScoped = "jakarta.transaction.TransactionScoped";
    private static final String applicationScoped = "jakarta.enterprise.context.ApplicationScoped";

    public void addBeans(AfterBeanDiscovery afterBeanDiscovery) {
        for (IntegrationWithCDIBagImpl integrationWithCDIBag : persistenceUnitIntegrationStuff) {
            PersistenceUnitMetadata persistenceUnitMetadata = integrationWithCDIBag.getPersistenceUnitMetadata();

            // determine the qualifiers to use for creating each bean
            List<String> qualifiers = Collections.emptyList();
            if (persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0) {
                qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
            }

            entityManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            entityManagerFactory(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            criteriaBuilder(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            persistenceUnitUtil(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            cache(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            metamodel(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);
            schemaManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, integrationWithCDIBag);

        }
        foundProducers.clear();
    }

    private void entityManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            IntegrationWithCDIBag integrationWithCDIBag) {

        if (producerExists(EntityManager.class, qualifiers)) {
            return;
        }

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

        if (producerExists(EntityManagerFactory.class, qualifiers)) {
            return;
        }

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

        if (producerExists(CriteriaBuilder.class, qualifiers)) {
            return;
        }

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

        if (producerExists(PersistenceUnitUtil.class, qualifiers)) {
            return;
        }

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

        if (producerExists(Cache.class, qualifiers)) {
            return;
        }

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

        if (producerExists(Metamodel.class, qualifiers)) {
            return;
        }

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

        if (producerExists(SchemaManager.class, qualifiers)) {
            return;
        }
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

    /**
     * Processes a bean looking for known injection types. If found, the type is captured along with the qualifiers.
     * The captured information is later used to determine if a producer already exists for the type and a producer
     * will not be created dynamically.
     *
     * @param bean the bean to check
     */
    private void processBeanProducer(final Bean<?> bean) {
        // Check for known injection types
        addProducerType(bean, EntityManager.class);
        addProducerType(bean, EntityManagerFactory.class);
        addProducerType(bean, CriteriaBuilder.class);
        addProducerType(bean, PersistenceUnitUtil.class);
        addProducerType(bean, Metamodel.class);
        addProducerType(bean, SchemaManager.class);
    }

    /**
     * Adds, if applicable, the type to the map of known injection types with the qualifiers for the bean.
     *
     * @param bean the bean being processed
     * @param type the injection type to look for
     */
    private void addProducerType(final Bean<?> bean, final Class<?> type) {
        if (bean.getTypes().contains(type)) {
            final var qualifiers = foundProducers.computeIfAbsent(type, ignore -> new ArrayList<>());
            qualifiers.add(bean
                    .getQualifiers()
                    .stream()
                    .map(a -> a.annotationType().getName())
                    .collect(Collectors.toSet()));
        }
    }

    /**
     * Checks if a producer for the type already exists. If the producer exists, a producer will not be dynamically
     * created.
     *
     * @param type the type to check if a producer exists
     * @param qualifiers the qualifiers for the producer
     *
     * @return {@code true} if a producer already exists, otherwise {@code false}
     */
    private boolean producerExists(final Class<?> type, final Collection<String> qualifiers) {
        final var allQualifiers = foundProducers.get(type);
        if (allQualifiers != null) {
            for (final var q : allQualifiers) {
                if (qualifiers.isEmpty()) {
                    if (DEFAULT_QUALIFIERS.containsAll(q)) {
                        return true;
                    }
                }
                if (q.containsAll(qualifiers)) {
                    return true;
                }
            }
        }
        return false;
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
