/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.java8;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that deployment does not fail with {@link InvalidBytecodeException} if EJB's local interface declares a static method (Java 8 feature).
 * See WFLY-4316 for details.
 *
 * @author Jozef Hartinger
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class Java8InterfacesEJBTestCase {

    @Inject
    private Airplane airplane;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addPackage(Java8InterfacesEJBTestCase.class.getPackage());
    }

    @Test
    public void testDeploymentWorks() {
        Assert.assertTrue(Airplane.barrelRoll());
        Assert.assertTrue(airplane.takeOff());
    }


    // See https://issues.jboss.org/browse/WFCORE-3512
    @Test
    public void testDefaultMethodWorks() {
        Assert.assertEquals("Cargo", airplane.getPlaneType());
    }
}
