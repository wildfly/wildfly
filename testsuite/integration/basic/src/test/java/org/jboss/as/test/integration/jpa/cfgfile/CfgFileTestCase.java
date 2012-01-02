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

package org.jboss.as.test.integration.jpa.cfgfile;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Hibernate configuration in hibernate.cfg.xml file
 *
 * @author Zbynek Roubalik
 */
@RunWith(Arquillian.class)
public class CfgFileTestCase {

    private static final String ARCHIVE_NAME = "jpa_cfgfile";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "		<properties> " +
            "			<property name=\"hibernate.ejb.cfgfile\" value=\"hibernate.cfg.xml\"/>" +
            "		</properties>" +
            "  </persistence-unit>" +
            "</persistence>"; 

  private static final String hibernate_cfg_xml =
		"<?xml version='1.0' encoding='utf-8'?>\n " +
		  "<!DOCTYPE hibernate-configuration PUBLIC\n" +
		  "\"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n" +
		  "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\n" +
		  "<hibernate-configuration>" +
		  	"<session-factory>" +
		  	"    <property name=\"connection.driver_class\">org.hsqldb.jdbcDriver</property>" +
		  	"    <property name=\"hibernate.connection.datasource\">java:jboss/datasources/ExampleDS</property>" +
		  	"    <property name=\"dialect\">org.hibernate.dialect.HSQLDialect</property>" +
		  	"    <property name=\"hbm2ddl.auto\">create-drop</property>" +
		  	"  </session-factory>" +
		  "</hibernate-configuration>";
    
    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(CfgFileTestCase.class,
            Employee.class,
            SFSB1.class
        );
        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addAsResource(new StringAsset(hibernate_cfg_xml), "hibernate.cfg.xml");
        return jar; 
    
    }

    @ArquillianResource
    private InitialContext iniCtx;


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    @Test
    public void testEntityManagerInvocation() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.getEmployeeNoTX(1);
    }
    
    @Test
    public void testProperties() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        Map<String, Object> props = sfsb1.getEMFProperties();

        assertEquals("Value for org.hsqldb.jdbcDriver", "org.hsqldb.jdbcDriver", props.get("connection.driver_class").toString());
        assertEquals("Value for hibernate.connection.datasource", "java:jboss/datasources/ExampleDS", props.get("hibernate.connection.datasource").toString());
        assertEquals("Value for dialect", "org.hibernate.dialect.HSQLDialect", props.get("dialect").toString());
    }
    
}
