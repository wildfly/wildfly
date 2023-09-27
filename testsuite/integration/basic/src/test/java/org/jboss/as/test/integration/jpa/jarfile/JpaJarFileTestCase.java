/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
