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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the <jar-file> element of persistence.xml works as expected.
 *
 * @author Stuart Douglas
 * @author Franck Garcia
 */
@RunWith(Arquillian.class)
public class JpaJarFileTestCase {

    private static final String ARCHIVE_NAME = "jpajarfile.ear";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);
        JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class, "my-ejb-module.jar");
        ejbModule.addClasses(JpaJarFileTestCase.class, JpaTestSlsb.class);
        ejbModule.addAsManifestResource(JpaJarFileTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbModule);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(JarFileEntity.class);
        jar.addClass(MainArchiveEntity.class);
        ear.addAsLibrary(jar);
        return ear;
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

}
