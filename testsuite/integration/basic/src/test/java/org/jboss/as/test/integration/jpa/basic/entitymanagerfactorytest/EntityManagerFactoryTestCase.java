/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic.entitymanagerfactorytest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.basic.Employee;
import org.jboss.as.test.integration.jpa.basic.SFSB1;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EntityManagerFactory tests
 *
 * @author Zbynek Roubalik
 */
@RunWith(Arquillian.class)
public class EntityManagerFactoryTestCase {

    private static final String ARCHIVE_NAME = "jpa_emfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(EntityManagerFactoryTestCase.class,
                SFSB1.class, Employee.class);
        jar.addAsManifestResource(EntityManagerFactoryTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup(name));

        } catch (NamingException e) {
            throw e;
        }
    }

    /**
     * Test that EntityManagerFactory can be bind to specified JNDI name
     */
    @Test
    public void testEntityManagerFactoryName() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Sally", "1 home street", 1);

        EntityManagerFactory emf = rawLookup("myEMF", EntityManagerFactory.class);
        assertNotNull("JNDI lookup of jboss.entity.manager.factory.jndi.name should return EntityManagerFactory", emf);

        EntityManager em = emf.createEntityManager();
        Employee emp = em.find(Employee.class, 1);
        assertTrue("Name read from EntityManager is Sally", "Sally".equals(emp.getName()));

    }

    /**
     * Test that EntityManager can be bound to specified JNDI name in persistence unit property jboss.entity.manager.jndi.name (AS7-6835)
     */
    @Test
    public void testEntityManagerName() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Sharon", "304 Bubbles Lane", 2);

        EntityManager em = rawLookup("java:/Manager1", EntityManager.class);
        assertNotNull("JNDI lookup of jboss.entity.manager.jndi.name should return EntityManager", em);

        Employee emp = em.find(Employee.class, 2);
        assertTrue("Name read from EntityManager is Sharon", "Sharon".equals(emp.getName()));

    }

}
