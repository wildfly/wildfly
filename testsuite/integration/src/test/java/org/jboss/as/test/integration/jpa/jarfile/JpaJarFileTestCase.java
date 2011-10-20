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

package org.jboss.as.test.integration.jpa.jarfile;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the <jar-file> element of persistence.xml works as expected.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class JpaJarFileTestCase {

    private static final String ARCHIVE_NAME = "jpajarfile.war";

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addClasses(JpaJarFileTestCase.class, JpaTestSlsb.class);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "main.jar");
        jar.addClass(MainArchiveEntity.class);
        jar.addAsManifestResource(getPersistenceXml(), "persistence.xml");
        war.addAsLibrary(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(JarFileEntity.class);
        war.addAsLibrary(jar);

        return war;
    }

    @Test
    public void testEntityInMainArchive() throws NamingException {
        JpaTestSlsb slsb = (JpaTestSlsb) new InitialContext().lookup("java:module/" + JpaTestSlsb.class.getSimpleName());
        slsb.testMainArchiveEntity();
    }

    @Test
    public void testEntityInJarFileArchive() throws NamingException {
        JpaTestSlsb slsb = (JpaTestSlsb) new InitialContext().lookup("java:module/" + JpaTestSlsb.class.getSimpleName());
        slsb.testJarFileEntity();
    }

    private static StringAsset getPersistenceXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
                "  <persistence-unit name=\"mainPu\">" +
                "  <jar-file>jarfile.jar</jar-file>" +
                "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
                "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
                "</properties>" +
                "  </persistence-unit>" +
                "</persistence>");
    }


}
