/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.rar;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.rar.ejb.NoOpEJB;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a .rar deployed within a .ear doesn't run into deployment problems
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-2111
 */
@RunWith(Arquillian.class)
public class RarDeploymentTestCase {

    /**
     * .ear
     * |
     * |--- helloworld.rar
     * |
     * |--- ejb.jar
     * |
     * |--- META-INF
     * |       |
     * |       |--- application.xml containing <module> <connector>helloworld.rar</connector> </module>
     *
     * @return
     */
    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClass(NoOpEJB.class);

        final JavaArchive rar = ShrinkWrap.create(JavaArchive.class, "helloworld.rar");
        rar.addPackage(HelloWorldResourceAdapter.class.getPackage());

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "rar-in-ear-test.ear");
        ear.addAsModule(rar);
        ear.addAsModule(ejbJar);
        ear.addAsManifestResource(RarDeploymentTestCase.class.getPackage(), "application.xml", "application.xml");

        return ear;
    }

    /**
     * Test the deployment succeeded.
     *
     * @throws Exception
     */
    @Test
    public void testDeployment() throws Exception {
        final NoOpEJB noOpEJB = InitialContext.doLookup("java:app/ejb/" + NoOpEJB.class.getSimpleName());
        noOpEJB.doNothing();
    }
}
