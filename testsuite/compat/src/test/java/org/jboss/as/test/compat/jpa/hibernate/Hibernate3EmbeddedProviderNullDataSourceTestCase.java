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
import org.jboss.as.test.compat.common.HttpRequest;
import org.jboss.as.test.compat.common.JndiUtil;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.jpa.JpaEmployeeBean;
import org.jboss.as.test.compat.common.SimpleServlet;
import org.jboss.as.test.compat.common.TestUtil;
import org.jboss.as.test.compat.common.WebLink;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.io.File;

/**
 * Test with no datasource specified
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class Hibernate3EmbeddedProviderNullDataSourceTestCase {

    private static final String ARCHIVE_NAME = "Hibernate3EmbeddedProviderNullDataSourceTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"test-compat-persistence-context\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "    <properties>" +
            "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "      <property name=\"hibernate.show_sql\" value=\"true\"/>" +
            "      <property name=\"jboss.as.jpa.providerModule\" value=\"hibernate3-bundled\"/>" +
            "      <property name=\"hibernate.dialect\" value=\"org.hibernate.dialect.H2Dialect\"/>\n" +
            "      <property name=\"hibernate.connection.driver_class\" value=\"org.h2.Driver\"/>\n" +
            "      <property name=\"hibernate.connection.url\" value=\"jdbc:h2:mem\" />\n" +
            "      <property name=\"hibernate.connection.username\" value=\"sa\"/>\n" +
            "      <property name=\"hibernate.connection.password\" value=\"sa\"/>\n" +
            "      <property name=\"hibernate.connection.autocommit\" value=\"false\" />" +
            "    </properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    private static final String web_persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"web_hibernate3_pc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "    <properties>" +
            "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "      <property name=\"hibernate.show_sql\" value=\"true\"/>" +
            "      <property name=\"jboss.as.jpa.providerModule\" value=\"hibernate3-bundled\"/>" +
            "      <property name=\"hibernate.dialect\" value=\"org.hibernate.dialect.H2Dialect\"/>\n" +
            "      <property name=\"hibernate.connection.driver_class\" value=\"org.h2.Driver\"/>\n" +
            "      <property name=\"hibernate.connection.url\" value=\"jdbc:h2:mem\" />\n" +
            "      <property name=\"hibernate.connection.username\" value=\"sa\"/>\n" +
            "      <property name=\"hibernate.connection.password\" value=\"sa\"/>\n" +
            "      <property name=\"hibernate.connection.autocommit\" value=\"false\" />" +
            "    </properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    private static void addHibernate3JarsToEar(EnterpriseArchive ear) {
        final String basedir = System.getProperty("basedir");
        final String testdir = basedir + File.separatorChar + "target" + File.separatorChar + "test-libs";
        File hibernatecore = new File(testdir, "hibernate3-core.jar");
        File hibernateannotations = new File(testdir, "hibernate3-commons-annotations.jar");
        File hibernateentitymanager = new File(testdir, "hibernate3-entitymanager.jar");
        ear.addAsLibraries(
            hibernatecore,
            hibernateannotations,
            hibernateentitymanager
        );

    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        addHibernate3JarsToEar(ear);

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(EmployeeBean.class, JpaEmployeeBean.class, HttpRequest.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(Hibernate3EmbeddedProviderNullDataSourceTestCase.class, JndiUtil.class, TestUtil.class);
        ear.addAsModule(main);

        // add war that contains its own pu
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(SimpleServlet.class, WebLink.class);
        war.addAsResource(new StringAsset(web_persistence_xml), "META-INF/persistence.xml");

        war.addAsWebInfResource(
                new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "\n" +
                        "<web-app version=\"3.0\"\n" +
                        "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                        "         metadata-complete=\"false\">\n" +
                        "<servlet-mapping>\n" +
                        "        <servlet-name>SimpleServlet</servlet-name>\n" +
                        "        <url-pattern>/simple/*</url-pattern>\n" +
                        "    </servlet-mapping>\n" +
                        "</web-app>"),
                "web.xml");

        ear.addAsModule(war);

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
    public void testSimpleCreateAndLoadEntities() throws Exception {
        final EmployeeBean employeeBean = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, JpaEmployeeBean.class, EmployeeBean.class);
        TestUtil.testSimpleCreateAndLoadEntities(employeeBean);
    }

    @Test
    public void testServletSubDeploymentRead() throws Exception {
        TestUtil.testServletSubDeploymentRead(ARCHIVE_NAME, "Hello+world");
    }


}
