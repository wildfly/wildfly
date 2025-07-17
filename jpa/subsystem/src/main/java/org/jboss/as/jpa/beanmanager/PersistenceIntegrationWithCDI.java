package org.jboss.as.jpa.beanmanager;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.inject.Qualifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
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


    public static void addBeans(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitMetadata persistenceUnitMetadata, TransactionSynchronizationRegistry transactionSynchronizationRegistry, TransactionManager transactionManager, CompletableFuture<EntityManagerFactory> futureEntityManagerFactory) {

        if (futureEntityManagerFactory == null) {
            throw new IllegalStateException("futureEntityManagerFactory must be specified earlier");
        }
        // determine the qualifiers to use for creating each bean
        List<String> qualifiers;
        if (persistenceUnitMetadata.getQualifierAnnotationNames().size() > 0) {
            qualifiers = persistenceUnitMetadata.getQualifierAnnotationNames();
        } else {
            qualifiers = defaultQualifier;
        }


        try {
            entityManager(afterBeanDiscovery, persistenceUnitMetadata, qualifiers, transactionSynchronizationRegistry, transactionManager, futureEntityManagerFactory);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void entityManager(
            AfterBeanDiscovery afterBeanDiscovery,
            PersistenceUnitMetadata persistenceUnitMetadata,
            List<String> qualifiers,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            TransactionManager transactionManager,
            CompletableFuture<EntityManagerFactory> futureEntityManagerFactory) throws InstantiationException, IllegalAccessException {

        String scope = persistenceUnitMetadata.getScopeAnnotationName();
        if (scope == null || scope.isEmpty()) {
            scope = transactionScoped;
        }

        // EntityManager setup
        BeanConfigurator beanConfigurator = afterBeanDiscovery.addBean();
        beanConfigurator.addType(EntityManager.class);

        try {
            if (!transactionScoped.equals(scope)) {
                Class<? extends Annotation> annotation = persistenceUnitMetadata.getClassLoader().loadClass(scope).asSubclass(Annotation.class);
                // Annotation actualAnnotation = annotation.getAnnotation(Scope.class);
                beanConfigurator.scope(annotation);
            }
            for (String qualifier : qualifiers) {
                Class<? extends Annotation> annotation = persistenceUnitMetadata.getClassLoader().loadClass(qualifier).asSubclass(Annotation.class);
                Annotation actualAnnotation = annotation.getAnnotation(Qualifier.class);
                beanConfigurator.addQualifier(actualAnnotation);
            }
            Class<?> transactionScopedEntityManager = TransactionScopedEntityManager.class;
            beanConfigurator.beanClass(transactionScopedEntityManager);

//            beanConfigurator.createWith(c -> {
//                        T instance = injectionTarget.produce(c);
//                        injectionTarget.inject(instance, c);
//                        injectionTarget.postConstruct(instance);
//                        return instance;
//                    });

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
