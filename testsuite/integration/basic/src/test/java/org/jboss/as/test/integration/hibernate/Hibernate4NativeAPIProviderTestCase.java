/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.hibernate;

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that a Hibernate sessionfactory can be inititated from hibernate.cfg.xml and properties added to Hibernate Configuration
 * in AS7 container without any JPA assistance
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class Hibernate4NativeAPIProviderTestCase {
    private static final String ARCHIVE_NAME = "hibernate4native_test";

    public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
            + "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
            + "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
            + "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">false</property>"
            + "<property name=\"current_session_context_class\">thread</property>"
// only needed for ORM 5.3.0    + "<property name=\"hibernate.allow_update_outside_transaction\">true</property>"
            + "<mapping resource=\"testmapping.hbm.xml\"/>" + "</session-factory></hibernate-configuration>";

    public static final String testmapping = "<?xml version=\"1.0\"?>" + "<!DOCTYPE hibernate-mapping PUBLIC "
            + "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" " + "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
            + "<hibernate-mapping package=\"org.jboss.as.test.integration.hibernate\">"
            + "<class name=\"org.jboss.as.test.integration.hibernate.Student\" table=\"STUDENT\">"
            + "<id name=\"studentId\" column=\"student_id\">" + "<generator class=\"native\"/>" + "</id>"
            + "<property name=\"firstName\" column=\"first_name\"/>" + "<property name=\"lastName\" column=\"last_name\"/>"
            + "<property name=\"address\"/>"
            // + "<set name=\"courses\" table=\"student_courses\">"
            // + "<key column=\"student_id\"/>"
            // + "<many-to-many column=\"course_id\" class=\"org.jboss.as.test.integration.nonjpa.hibernate.Course\"/>"
            // + "</set>" +
            + "</class></hibernate-mapping>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        // add required jars as manifest dependencies
        ear.addAsManifestResource(new StringAsset("Dependencies: org.hibernate\n"), "MANIFEST.MF");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SFSBHibernateSessionFactory.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Student.class);
        lib.addAsResource(new StringAsset(testmapping), "testmapping.hbm.xml");
        lib.addAsResource(new StringAsset(hibernate_cfg), "hibernate.cfg.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(Hibernate4NativeAPIProviderTestCase.class);
        ear.addAsModule(main);

        // add application dependency on H2 JDBC driver, so that the Hibernate classloader (same as app classloader)
        // will see the H2 JDBC driver.
        // equivalent hack for use of shared Hiberante module, would be to add the H2 dependency directly to the
        // shared Hibernate module.
        // also add dependency on org.slf4j
        ear.addAsManifestResource(new StringAsset("<jboss-deployment-structure>" + " <deployment>" + " <dependencies>"
                + " <module name=\"com.h2database.h2\" />" + " <module name=\"org.slf4j\"/>" + " </dependencies>"
                + " </deployment>" + "</jboss-deployment-structure>"), "jboss-deployment-structure.xml");

        return ear;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!"
                    + interfaceType.getName()));
        } catch (NamingException e) {
            throw e;
        }
    }

    @Test
    public void testSimpleOperation() throws Exception {
        SFSBHibernateSessionFactory sfsb = lookup("SFSBHibernateSessionFactory", SFSBHibernateSessionFactory.class);
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            Student s1 = sfsb.createStudent("MADHUMITA", "SADHUKHAN", "99 Purkynova REDHAT BRNO CZ", 1);
            Student s2 = sfsb.createStudent("REDHAT", "LINUX", "Worldwide", 3);
            Student st = sfsb.getStudent(s1.getStudentId());
            assertTrue("name read from hibernate session is MADHUMITA", "MADHUMITA".equals(st.getFirstName()));
        } finally {
            sfsb.cleanup();
        }
    }
}
