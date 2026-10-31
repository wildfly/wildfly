/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.persistence.cdi;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the required CDI producers work as expected.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@ApplicationScoped
public class RequiredInjectionTestCase {

    private static final String DEPLOYMENT_NAME = RequiredInjectionTestCase.class.getSimpleName() + ".war";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(Employee.class)
                .addAsWebInfResource(RequiredInjectionTestCase.class.getPackage(), "simple-persistence.xml", "classes/META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private EntityManager entityManager;

    @Inject
    private EntityManagerFactory entityManagerFactory;

    @Inject
    private CriteriaBuilder criteriaBuilder;

    @Inject
    private PersistenceUnitUtil persistenceUnitUtil;

    @Inject
    private Metamodel metamodel;

    @Inject
    private SchemaManager schemaManager;

    @Inject
    private BeanManager beanManager;

    @Test
    public void checkEntityManager() throws Exception {
        // The default scope is TransactionScoped which means we need to being a transaction before injection will work
        userTransaction.begin();
        try {
            Assertions.assertNotNull(entityManager);
            Assertions.assertEquals(DEPLOYMENT_NAME + "#default", entityManager.getEntityManagerFactory().getName());
        } finally {
            if (userTransaction.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
    }

    @Test
    public void checkEntityManagerProducer() throws Exception {
        // The default scope is TransactionScoped which means we need to being a transaction before injection will work
        userTransaction.begin();
        try {
            final Set<Bean<?>> beans = beanManager.getBeans(EntityManager.class);
            // We should only have one bean which is a producer method
            Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
            final Bean<?> bean = beans.iterator().next();
            Assertions.assertEquals(EntityManager.class, bean.getBeanClass());
        } finally {
            if (userTransaction.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
    }

    @Test
    public void checkEntityManagerFactory() {
        Assertions.assertNotNull(entityManagerFactory);
        Assertions.assertEquals(DEPLOYMENT_NAME + "#default", entityManagerFactory.getName());
    }

    @Test
    public void checkEntityManagerFactoryProducer() {
        final Set<Bean<?>> beans = beanManager.getBeans(EntityManagerFactory.class);
        // We should only have one bean which is a producer method
        Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
        final Bean<?> bean = beans.iterator().next();
        Assertions.assertEquals(EntityManagerFactory.class, bean.getBeanClass());
    }

    @Test
    public void checkCriteriaBuilder() {
        Assertions.assertNotNull(criteriaBuilder);
    }

    @Test
    public void checkCriteriaBuilderProducer() {
        final Set<Bean<?>> beans = beanManager.getBeans(CriteriaBuilder.class);
        // We should only have one bean which is a producer method
        Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
        final Bean<?> bean = beans.iterator().next();
        Assertions.assertEquals(CriteriaBuilder.class, bean.getBeanClass());
    }

    @Test
    public void checkPersistenceUnitUtil() {
        Assertions.assertNotNull(persistenceUnitUtil);
    }

    @Test
    public void checkPersistenceUnitUtilProducer() {
        final Set<Bean<?>> beans = beanManager.getBeans(PersistenceUnitUtil.class);
        // We should only have one bean which is a producer method
        Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
        final Bean<?> bean = beans.iterator().next();
        Assertions.assertEquals(PersistenceUnitUtil.class, bean.getBeanClass());
    }

    @Test
    public void checkMetamodel() {
        Assertions.assertNotNull(metamodel);
    }

    @Test
    public void checkMetamodelProducer() {
        final Set<Bean<?>> beans = beanManager.getBeans(Metamodel.class);
        // We should only have one bean which is a producer method
        Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
        final Bean<?> bean = beans.iterator().next();
        Assertions.assertEquals(Metamodel.class, bean.getBeanClass());
    }

    @Test
    public void checkSchemaManager() {
        Assertions.assertNotNull(schemaManager);
    }

    @Test
    public void checkSchemaManagerProducer() {
        final Set<Bean<?>> beans = beanManager.getBeans(SchemaManager.class);
        // We should only have one bean which is a producer method
        Assertions.assertEquals(1, beans.size(), String.format("Expected 1 bean, but got %s", beans));
        final Bean<?> bean = beans.iterator().next();
        Assertions.assertEquals(SchemaManager.class, bean.getBeanClass());
    }
}
