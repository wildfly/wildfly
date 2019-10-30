/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.altdd;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the alt-dd element of application.xml is respected
 */
@RunWith(Arquillian.class)
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
        Assert.assertEquals("alternate", bean.getValue());
    }

}
