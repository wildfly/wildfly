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

package org.jboss.as.test.compat.jpa.hibernate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.compat.common.EmployeeBean;
import org.jboss.as.test.compat.common.JndiUtil;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.jpa.JpaCacheEmployeeBean;
import org.jboss.as.test.compat.common.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.io.File;

/**
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class Hibernate3EmbeddedProviderSecondLevelCacheTestCase {

    private static final String ARCHIVE_NAME = "Hibernate3EmbeddedProviderSecondLevelCacheTestCase";

    private static final String DATASOURCE = "java:jboss/datasources/ExampleDS";

    private static final String persistence_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                    "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
                    "  <persistence-unit name=\"test-compat-persistence-context\">" +
                    "    <description>Persistence Unit</description>" +
                    "    <jta-data-source>" + DATASOURCE + "</jta-data-source>" +
                    "    <shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>" +
                    "    <properties>" +
                    "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
                    "      <property name=\"hibernate.show_sql\" value=\"true\"/>" +
                    "      <property name=\"hibernate.cache.use_second_level_cache\" value=\"true\"/>" +
                    "      <property name=\"hibernate.cache.use_query_cache\" value=\"true\"/>" +
                    "      <property name=\"hibernate.generate_statistics\" value=\"true\"/>" +
                    "      <property name=\"jboss.as.jpa.providerModule\" value=\"hibernate3-bundled\"/>" +
                    "    </properties>" +
                    "  </persistence-unit>" +
                    "</persistence>";

    private static void addHibernate3JarsToEar(final EnterpriseArchive ear) {
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
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        addHibernate3JarsToEar(ear);

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(EmployeeBean.class, JpaCacheEmployeeBean.class);
        ear.addAsModule(lib);

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        jar.addClasses(Employee.class, Hibernate3EmbeddedProviderSecondLevelCacheTestCase.class, JndiUtil.class, TestUtil.class);
        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        ear.addAsLibraries(jar);

        // add application dependency on H2 JDBC driver, so that the Hibernate classloader (same as app classloader)
        // will see the H2 JDBC driver.
        // equivalent hack for use of shared Hiberante module, would be to add the H2 dependency directly to the
        // shared Hibernate module.
        // also add dependency on org.slf4j
        ear.addAsManifestResource(new StringAsset(
            "<jboss-deployment-structure>" +
            " <deployment>" +
            "  <dependencies>" +
            "   <module name=\"com.h2database.h2\" />" +
            "   <module name=\"org.antlr\" />" +
            "   <module name=\"org.apache.commons.collections\" />" +
            "   <module name=\"org.dom4j\" />" +
            "   <module name=\"org.infinispan\" />" +
            "   <module name=\"org.slf4j\"/>" +
            "  </dependencies>" +
            " </deployment>" +
            "</jboss-deployment-structure>"),
            "jboss-deployment-structure.xml");

        return ear;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        final EmployeeBean employeeBean = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, JpaCacheEmployeeBean.class, EmployeeBean.class);
        TestUtil.testSecondLevelCache(iniCtx, DATASOURCE, employeeBean);
    }
}