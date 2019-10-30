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
