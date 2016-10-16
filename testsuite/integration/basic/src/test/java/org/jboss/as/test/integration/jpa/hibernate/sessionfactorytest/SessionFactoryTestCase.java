/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.hibernate.sessionfactorytest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.Employee;
import org.jboss.as.test.integration.jpa.hibernate.SFSB1;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSession;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSessionFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate session factory tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class SessionFactoryTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(SessionFactoryTestCase.class,
                Employee.class,
                SFSB1.class,
                SFSBHibernateSession.class,
                SFSBHibernateSessionFactory.class
        );

        jar.addAsManifestResource(SessionFactoryTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
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

    // test that we didn't break the Hibernate hibernate.session_factory_name (bind Hibernate session factory to
    // specified jndi name) functionality.
    @Test
    public void testHibernateSessionFactoryName() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Sally", "1 home street", 1);

        // check if we can look up the Hibernate session factory that should of been bound because of
        // the hibernate.session_factory_name was specified in the properties (in peristence.xml above).
        SessionFactory hibernateSessionFactory = rawLookup("modelSessionFactory", SessionFactory.class);
        assertNotNull("jndi lookup of hibernate.session_factory_name should return HibernateSessionFactory", hibernateSessionFactory);

        Session session = hibernateSessionFactory.openSession();
        Employee emp = session.get(Employee.class, 1);
        assertTrue("name read from hibernate session is Sally", "Sally".equals(emp.getName()));
    }

    // Test that an extended Persistence context can be injected into a Hibernate Session
    // We use extended persistence context, otherwise the Hibernate session will be closed after each transaction and
    // the assert test would fail (due to lazy loading of the Employee entity.
    // Using extended persistence context allows the hibernate session to stay open long enough for the lazy fetch.
    @Test
    public void testInjectPCIntoHibernateSession() throws Exception {
        SFSBHibernateSession sfsbHibernateSession = lookup("SFSBHibernateSession", SFSBHibernateSession.class);
        sfsbHibernateSession.createEmployee("Molly", "2 apple way", 2);

        Employee emp = sfsbHibernateSession.getEmployee(2);
        assertTrue("name read from hibernate session is Molly", "Molly".equals(emp.getName()));
    }

    // Test that a Persistence unit can be injected into a Hibernate Session factory
    @Test
    public void testInjectPUIntoHibernateSessionFactory() throws Exception {
        SFSBHibernateSessionFactory sfsbHibernateSessionFactory =
                lookup("SFSBHibernateSessionFactory", SFSBHibernateSessionFactory.class);
        sfsbHibernateSessionFactory.createEmployee("Sharon", "3 beach ave", 3);

        Employee emp = sfsbHibernateSessionFactory.getEmployee(3);
        assertTrue("name read from hibernate session is Sharon", "Sharon".equals(emp.getName()));
    }

}
