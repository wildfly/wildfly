/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.persistence.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.Cache;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
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
        assertNotNull("expected nonnull SchemaManager", schemaManager);
    }

    @Test
    public void testEntityManagerNoResultExpected() throws Exception {

        Employee emp = cmtBean.getEmployeeExpectNullResult(101);
        assertNull("expected null result ", emp);
    }

    @Test
    public void testEntityManagerFactory() throws Exception {

            EntityManagerFactory emf = cmtBean.injectedEntityManagerFactory();
            assertNotNull("expected nonnull EntityManagerFactory ", emf);
        }

    @Test
    public void testEMFOfEntityManagerEqualEntityManagerFactory() throws Exception {

            EntityManagerFactory emf = cmtBean.injectedEntityManagerFactory(); // will be proxy object for EntityManagerFactory
            EntityManagerFactory emfOfEntityManager = cmtBean.entityManagerFactoryOfEntityManager(); // should be actual EntityManagerFactory returned by call to EntityManager.getEntityManagerFactory
            assertEquals("expected that EntityManagerFactory.getName() (persistence unit name) is same as EntityManager.getEntityManagerFactory.getName()", emfOfEntityManager.getName(), emf.getName());
        }

    @Test
    public void testCriteriaBuilder() throws Exception {
        CriteriaQuery criteriaQuery = cmtBean.testCreateQuery();
        assertNotNull("Created CriteriaQuery should of been returned", criteriaQuery);
    }

    @Test
    public void testPersistenceUnitUtil() throws Exception {
        PersistenceUnitUtil persistenceUnitUtil = cmtBean.testPersistenceUnitUtil();
        assertNotNull("PersistenceUnitUtil should of been returned", persistenceUnitUtil);
    }

    @Test
    public void testCache() throws Exception {
        Cache cache = cmtBean.testCache();
        assertNotNull("Cache should of been returned", cache);
    }

    @Test
    public void testMetamodel() throws Exception {
        Metamodel metamodel = cmtBean.testMetamodel();
        assertNotNull("Metamodel should of been returned", metamodel);

    }

    @Test
    public void testEmApplicationExistingProducer() throws Exception {
        EntityManager emApplicationExistingProducer = cmtBean.getEmApplicationExistingProducer();
        assertNotNull("Legacy producer didn't produce the EntityManager instance", emApplicationExistingProducer);
    }

    @Test
    public void testDefaultPersistenceUnit() throws Exception {
        EntityManagerFactory emf = cmtBean.getDefaultEntityManagerFactory();
        assertNotNull("Correct default persistence unit is injected that has property \"wildfly.jpa.default-unit\" set to true", emf);
        assertTrue("Default persistence unit name should be defaultPersistenceUnit", emf.getName().contains("defaultPersistenceUnit"));
    }

}
