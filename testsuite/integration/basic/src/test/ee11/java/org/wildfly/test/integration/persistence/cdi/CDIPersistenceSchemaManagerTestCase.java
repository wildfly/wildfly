/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.persistence.cdi;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.Cache;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

/**
 * Tests that CDI Persistence integration features works as described in
 * (web page) Persistence/CDI integration as mentioned in jakarta.ee/specifications/platform/11/jakarta-platform-spec-11.0#a441.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class CDIPersistenceSchemaManagerTestCase {

    private static final String ARCHIVE_NAME = "CDIPersistenceSchemaManagerTestCase";

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "CDIPersistenceSchemaManagerTestCase.jar");
        jar.addClasses(CDIPersistenceSchemaManagerTestCase.class, Employee.class, Pu1Qualifier.class, Pu2Qualifier.class, RequestScopedTestBean.class);
        jar.addAsManifestResource(CDIPersistenceSchemaManagerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(CDIPersistenceSchemaManagerTestCase.class.getPackage(), "beans.xml", "beans.xml");
        return jar;
    }

    @Inject
    RequestScopedTestBean cmtBean;

    @Test
    public void TestSchemaManager() throws Exception {
        SchemaManager schemaManager = cmtBean.testSchemaManager();
        assertNotNull(schemaManager, "expected nonnull SchemaManager");
    }

    @Test
    public void testEntityManagerNoResultExpected() throws Exception {

        Employee emp = cmtBean.getEmployeeExpectNullResult(101);
        assertNull(emp, "expected null result ");
    }

    @Test
    public void testEntityManagerFactory() throws Exception {

            EntityManagerFactory emf = cmtBean.injectedEntityManagerFactory();
            assertNotNull(emf, "expected nonnull EntityManagerFactory ");
        }

    @Test
    public void testEMFOfEntityManagerEqualEntityManagerFactory() throws Exception {

            EntityManagerFactory emf = cmtBean.injectedEntityManagerFactory(); // will be proxy object for EntityManagerFactory
            EntityManagerFactory emfOfEntityManager = cmtBean.entityManagerFactoryOfEntityManager(); // should be actual EntityManagerFactory returned by call to EntityManager.getEntityManagerFactory
            assertEquals(emfOfEntityManager.getName(), emf.getName(), "expected that EntityManagerFactory.getName() (persistence unit name) is same as EntityManager.getEntityManagerFactory.getName()");
        }

    @Test
    public void testCriteriaBuilder() throws Exception {
        CriteriaQuery criteriaQuery = cmtBean.testCreateQuery();
        assertNotNull(criteriaQuery, "Created CriteriaQuery should of been returned");
    }

    @Test
    public void testPersistenceUnitUtil() throws Exception {
        PersistenceUnitUtil persistenceUnitUtil = cmtBean.testPersistenceUnitUtil();
        assertNotNull(persistenceUnitUtil, "PersistenceUnitUtil should of been returned");
    }

    @Test
    public void testCache() throws Exception {
        Cache cache = cmtBean.testCache();
        assertNotNull(cache, "Cache should of been returned");
    }

    @Test
    public void testMetamodel() throws Exception {
        Metamodel metamodel = cmtBean.testMetamodel();
        assertNotNull(metamodel, "Metamodel should of been returned");

    }

    @Test
    public void testEmApplicationExistingProducer() throws Exception {
        EntityManager emApplicationExistingProducer = cmtBean.getEmApplicationExistingProducer();
        assertNotNull(emApplicationExistingProducer, "Legacy producer didn't produce the EntityManager instance");
    }

    @Test
    public void testDefaultPersistenceUnit() throws Exception {
        EntityManagerFactory emf = cmtBean.getDefaultEntityManagerFactory();
        assertNotNull(emf, "Correct default persistence unit is injected that has property \"wildfly.jpa.default-unit\" set to true");
        assertTrue(emf.getName().contains("defaultPersistenceUnit"), "Default persistence unit name should be defaultPersistenceUnit");
    }

}
