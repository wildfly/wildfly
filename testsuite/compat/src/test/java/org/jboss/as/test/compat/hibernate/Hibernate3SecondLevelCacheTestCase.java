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

package org.jboss.as.test.compat.hibernate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.common.EmployeeBean;
import org.jboss.as.test.compat.common.JndiUtil;
import org.jboss.as.test.compat.common.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;

/**
 * Test that Hibernate second level cache is working native Hibernate
 *
 * @author Scott Marlow (based on Madhumita's Hibernate test)
 */
@RunWith(Arquillian.class)
public class Hibernate3SecondLevelCacheTestCase {

    private static final String ARCHIVE_NAME = "hibernate3SecondLevelTest";

    public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
        + "<!DOCTYPE hibernate-configuration PUBLIC \"//Hibernate/Hibernate Configuration DTD 3.0//EN\" \"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
        + "<hibernate-configuration>"
        + "    <session-factory>"
        + "        <property name=\"show_sql\">true</property>"
        + "        <property name=\"hibernate.cache.use_second_level_cache\">true</property>"
        + "        <property name=\"hibernate.show_sql\">true</property>"
        + "        <property name=\"hibernate.cache.region.factory_class\">org.hibernate.cache.infinispan.JndiInfinispanRegionFactory</property>"
        + "        <property name=\"hibernate.cache.infinispan.cachemanager\">java:jboss/infinispan/container/hibernate</property>"
        + "        <property name=\"hibernate.transaction.manager_lookup_class\">org.hibernate.transaction.JBossTransactionManagerLookup</property>"
        + "        <mapping resource=\"testmapping.hbm.xml\"/>"
        + "    </session-factory>"
        + "</hibernate-configuration>";

    public static final String testmapping = "<?xml version=\"1.0\"?>"
        + "<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
        + "<hibernate-mapping package=\"org.jboss.as.test.compat.common\">"
        + "    <class name=\"" + Employee.class.getName() + "\" lazy=\"false\" table=\"employee\">"
        + "        <cache usage=\"transactional\"/>"
        + "        <id name=\"id\" column=\"id\">"
        + "            <generator class=\"native\"/>"
        + "        </id>"
        + "        <property name=\"name\" column=\"name\"/>"
        + "        <property name=\"address\" column=\"address\"/>"
        + "    </class>"
        + "</hibernate-mapping>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        final String basedir = System.getProperty("basedir");
        final String testdir = basedir + File.separatorChar + "target" + File.separatorChar + "test-libs";
        final File hibernateCore = new File(testdir, "hibernate3-core.jar");
        final File hibernateAnnotations = new File(testdir, "hibernate3-commons-annotations.jar");
        final File hibernateEntitymanager = new File(testdir, "hibernate3-entitymanager.jar");
        final File hibernateInfinispan = new File(testdir, "hibernate3-infinispan.jar");
        ear.addAsLibraries(
                hibernateCore,
                hibernateAnnotations,
                hibernateEntitymanager,
                hibernateInfinispan
        );

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(EmployeeBean.class, HibernateEmployeeBean.class);
        ear.addAsModule(lib);

        final JavaArchive entities = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        entities.addClasses(Employee.class);
        entities.addAsResource(new StringAsset(testmapping), "testmapping.hbm.xml");
        entities.addAsResource(new StringAsset(hibernate_cfg), "hibernate.cfg.xml");
        ear.addAsLibraries(entities);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(Hibernate3SecondLevelCacheTestCase.class, JndiUtil.class, TestUtil.class);
        ear.addAsModule(main);

        ear.addAsManifestResource(new StringAsset("<jboss-deployment-structure>\n"
                + "    <ear-subdeployments-isolated>false</ear-subdeployments-isolated>\n"
                + "    <deployment>\n"
                + "        <dependencies>\n"
                + "            <module name=\"com.h2database.h2\" />\n"
                + "            <module name=\"org.antlr\" />\n"
                + "            <module name=\"org.apache.commons.collections\" />\n"
                + "            <module name=\"org.dom4j\" />\n"
                + "            <module name=\"org.infinispan\"/>\n"
                + "            <module name=\"org.slf4j\"/>\n"
                + "        </dependencies>\n"
                + "    </deployment>\n"
                + "</jboss-deployment-structure>"), "jboss-deployment-structure.xml");

        return ear;
    }

    @Test
    public void testSecondLevelCache() throws Exception {
        final EmployeeBean employeeBean = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, HibernateEmployeeBean.class, EmployeeBean.class);
        TestUtil.testSecondLevelCache(iniCtx, HibernateEmployeeBean.DATASOURCE, employeeBean);

    }
}
