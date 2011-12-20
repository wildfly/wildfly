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

package org.jboss.as.test.compat.jpa.toplink;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@Ignore  // only for manual testing currently
public class TopLinkSharedModuleProviderTestCase {

    private static final String ARCHIVE_NAME = "toplink_module_test";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "    <persistence-unit name=\"test-compat-persistence-context\">" +
            "        <provider>oracle.toplink.essentials.PersistenceProvider</provider>"+
            "        <description>TopLink Persistence Unit</description>" +
            "        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
// uncomment after AS7-886 is fixed
//            "        <class>org.jboss.as.test.compat.common.Employee</class>" +
            "    </persistence-unit>" +
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
        main.addClasses(TopLinkSharedModuleProviderTestCase.class, JndiUtil.class, TestUtil.class);
        ear.addAsModule(main);

        return ear;
    }

    @Test
    public void testLoadTSJavaLogClassInTopLinkProviderJar() throws Exception {
        Employee.class.getClassLoader().loadClass("com.sun.jpalog.TSJavaLog");
        // success is when loadClass() didn't throw an exception
    }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        final EmployeeBean employeeBean = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, JpaEmployeeBean.class, EmployeeBean.class);
        TestUtil.testSimpleCreateAndLoadEntities(employeeBean);
    }

}
