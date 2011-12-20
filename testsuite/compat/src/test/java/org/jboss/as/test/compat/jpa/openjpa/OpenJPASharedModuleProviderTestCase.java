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

package org.jboss.as.test.compat.jpa.openjpa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.compat.common.EmployeeBean;
import org.jboss.as.test.compat.common.JndiUtil;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.jpa.JpaEmployeeBean;
import org.jboss.as.test.compat.common.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import javax.naming.InitialContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Create org.apache.openjpa:main module before running this test. The module
 * must depend on javaee.api and org.jboss.as.jpa.openjpa.
 *
 * @author Antti Laisi
 */
@RunWith(Arquillian.class)
@Ignore  // only for manual testing currently
public class OpenJPASharedModuleProviderTestCase {

    private static final String ARCHIVE_NAME = "openjpa_module_test";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
        "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
        "   <persistence-unit name=\"test-compat-persistence-context\">" +
        "       <provider>org.apache.openjpa.persistence.PersistenceProviderImpl</provider>"+
        "       <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
        "       <properties>" +
        "           <property name=\"openjpa.jdbc.SynchronizeMappings\" value=\"buildSchema(SchemaAction='drop,add')\"/>" +
        "           <property name=\"openjpa.InitializeEagerly\" value=\"true\"/>" +
        "           <property name=\"openjpa.RuntimeUnenhancedClasses\" value=\"supported\"/>" +
        "           <property name=\"jboss.as.jpa.providerModule\" value=\"org.apache.openjpa\"/>" +
        "           <property name=\"jboss.as.jpa.adapterModule\" value=\"org.jboss.as.jpa.openjpa\"/>" +
        "       </properties>" +
        "  </persistence-unit>" +
        "</persistence>";

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(EmployeeBean.class, JpaEmployeeBean.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        ear.addAsLibraries(lib);

        WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(OpenJPASharedModuleProviderTestCase.class, JndiUtil.class, TestUtil.class);
        ear.addAsModule(main);

        return ear;
    }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        final JpaEmployeeBean employeeBean = InitialContext.doLookup("java:global/" + ARCHIVE_NAME + "/beans/JpaEmployeeBean");
        TestUtil.testSimpleCreateAndLoadEntities(employeeBean);
    }

}
