/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.excludesubsystem;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests excluding a subsystem via jboss-deployment-structure.xml
 */
@ExtendWith(ArquillianExtension.class)
public class ExcludeEjbSubsystemTestCase {

    private static final Logger logger = Logger.getLogger(ExcludeEjbSubsystemTestCase.class);

    @ArquillianResource
    private javax.naming.InitialContext initialContext;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "excludeSubsystem.jar");
        jar.addAsManifestResource(ExcludeEjbSubsystemTestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
        jar.addPackage(ExcludeEjbSubsystemTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testEjbNotInstalled() throws NamingException {
        try {
            Object result = initialContext.lookup("java:module/" + SimpleEjb.class.getSimpleName());
            Assertions.fail("Expected lookup to fail, instead " + result + " was returned");
        } catch (NameNotFoundException expected) {}
    }


}
