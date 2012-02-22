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

package org.jboss.as.test.integration.hibernate.secondlevelcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

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
 * Test that Hibernate second level cache is working native Hibernate
 *
 * @author Scott Marlow (based on Madhumita's Hibernate test)
 */
@RunWith(Arquillian.class)
public class HibernateSecondLevelCacheTestCase {

    private static final String FACTORY_CLASS="<property name=\"hibernate.cache.region.factory_class\">org.jboss.as.jpa.hibernate4.infinispan.InfinispanRegionFactory</property>";
    private static final String MODULE_DEPENDENCIES ="Dependencies: org.infinispan,org.hibernate.envers export,org.hibernate\n";

    private static final String ARCHIVE_NAME = "hibernateSecondLevel_test";

    public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
        + "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
        + "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
        + "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">true</property>"
        + "<property name=\"hibernate.cache.use_second_level_cache\">true</property>"
        + "<property name=\"hibernate.show_sql\">true</property>"
        + FACTORY_CLASS
        + "<property name=\"hibernate.cache.infinispan.cachemanager\">java:jboss/infinispan/container/hibernate</property>"
        + "<mapping resource=\"testmapping.hbm.xml\"/>" + "</session-factory></hibernate-configuration>";

    public static final String testmapping = "<?xml version=\"1.0\"?>" + "<!DOCTYPE hibernate-mapping PUBLIC "
        + "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" " + "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
        + "<hibernate-mapping package=\"org.jboss.as.test.integration.hibernate\">"
        + "<class name=\"" + Student.class.getName() + "\" lazy=\"false\" table=\"STUDENT\">"
        + "<cache usage=\"transactional\"/>"
        + "<id name=\"studentId\" column=\"student_id\">" + "<generator class=\"native\"/>" + "</id>"
        + "<property name=\"firstName\" column=\"first_name\"/>" + "<property name=\"lastName\" column=\"last_name\"/>"
        + "<property name=\"address\"/>"
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
                ear.addAsManifestResource(new StringAsset(MODULE_DEPENDENCIES), "MANIFEST.MF");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SFSB.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Student.class);
        lib.addAsResource(new StringAsset(testmapping), "testmapping.hbm.xml");
        lib.addAsResource(new StringAsset(hibernate_cfg), "hibernate.cfg.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(HibernateSecondLevelCacheTestCase.class);
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
            dumpJndi("");
            throw e;
        }
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private static void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private static void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(ncp.getName());
            } else {
                dumpJndi(s + "/" + ncp.getName());
            }
        }
    }

    @Test
    public void testSecondLevelCache() throws Exception {
        SFSB sfsb = lookup("SFSB",
            SFSB.class);
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        Student s1 = sfsb.createStudent("MADHUMITA", "SADHUKHAN", "99 Purkynova REDHAT BRNO CZ", 1);
        Student s2 = sfsb.getStudent(1);

        DataSource ds = rawLookup("java:jboss/datasources/ExampleDS", DataSource.class);
        Connection conn = ds.getConnection();
        System.out.println("got JDBC connection");
        int updated = conn.prepareStatement("update Student set first_name='hacked' where student_id=1").executeUpdate();
        assertTrue("was able to update added Student.  update count=" + updated, updated > 0);
        conn.close();
        System.out.println("updated student first name to 'hacked' (bypassing 2lc by executing sql directly");
        // read updated (dirty) data from second level cache
        s2 = sfsb.getStudent(1);
        System.out.println("get sfsb.getStudent() returned student with first name=" + s2.getFirstName());
        assertTrue("was able to read updated Student entity", s2 != null);
        assertEquals("Student first name was read from second level cache = " + s2.getFirstName(), "MADHUMITA", s2.getFirstName());

    }
}
