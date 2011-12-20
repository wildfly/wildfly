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
import org.jboss.as.test.compat.jpa.JpaEmployeeBean;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.common.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@Ignore  // until we hack the test to populate the org.hibernate module
public class Hibernate3SharedModuleProviderTestCase {

    private static final String ARCHIVE_NAME = "hibernate3module_test";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"test-compat-persistence-context\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<class>org.jboss.as.test.compat.common.Employee</class>" +  // currently hibernate 3.3.x cannot discover entities
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "<property name=\"hibernate.show_sql\" value=\"true\"/>" +
// set the providerModule to the AS org.hibernate3 module (should be in as/modules/org/hibernate/3 folder
// Hibernate 3 jars will need to be in the module folder and each jar name
// should be added to module.xml in same folder.
// not configuring the org.hibernate:3 module will give errors like this: http://pastie.org/2280769
            "<property name=\"jboss.as.jpa.providerModule\" value=\"org.hibernate:3\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

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

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(Hibernate3SharedModuleProviderTestCase.class, JndiUtil.class, TestUtil.class);
        ear.addAsModule(main);

        return ear;
    }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        final EmployeeBean employeeBean = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, JpaEmployeeBean.class, EmployeeBean.class);
        TestUtil.testSimpleCreateAndLoadEntities(employeeBean);
    }

}
