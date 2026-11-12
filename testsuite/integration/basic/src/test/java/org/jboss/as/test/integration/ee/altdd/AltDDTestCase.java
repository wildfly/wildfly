/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.altdd;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the alt-dd element of application.xml is respected
 */
@ExtendWith(ArquillianExtension.class)
public class AltDDTestCase {

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "altdd.ear");
        ear.addAsManifestResource(AltDDTestCase.class.getPackage(), "application.xml", "application.xml");
        ear.addAsResource(AltDDTestCase.class.getPackage(), "alt-ejb-jar.xml",  "alt-ejb-jar.xml");

        final JavaArchive ejbs = ShrinkWrap.create(JavaArchive.class,"ejb.jar");
        ejbs.addClasses(AltDDTestCase.class, AltDDEjb.class);
        ejbs.addAsManifestResource(AltDDTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ear.addAsModule(ejbs);
        return ear;
    }

    @Test
    public void testAlternateDeploymentDescriptor() throws NamingException {
        final AltDDEjb bean = (AltDDEjb) new InitialContext().lookup("java:module/" + AltDDEjb.class.getSimpleName());
        Assertions.assertEquals("alternate", bean.getValue());
    }

}
