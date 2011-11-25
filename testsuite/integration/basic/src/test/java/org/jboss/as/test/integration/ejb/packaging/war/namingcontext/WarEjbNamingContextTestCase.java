/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.packaging.war.namingcontext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that EJB's packaged in a war use the correct naming context
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WarEjbNamingContextTestCase {


    private static final String ARCHIVE_NAME = "WarEjbNamingContextTestCase";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClass(EjbInterface.class);
        ear.addAsLibrary(lib);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war");
        war1.addClasses(WarEjbNamingContextTestCase.class, War1Ejb.class);
        ear.addAsModule(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war");
        war2.addClasses(War2Ejb.class);
        ear.addAsModule(war2);
        return ear;
    }


    @Test
    public void testCorrectNamingContextUsedForEjbInWar() throws Exception {
        EjbInterface ejb = (EjbInterface) new InitialContext().lookup("java:app/war1/War1Ejb");
        Assert.assertNotNull(ejb.lookupUserTransaction());
        try {
            ejb.lookupOtherUserTransaction();
            Assert.fail();
        } catch (NamingException expected) {

        }

        ejb = (EjbInterface) new InitialContext().lookup("java:app/war2/War2Ejb");
        Assert.assertNotNull(ejb.lookupUserTransaction());
        try {
            ejb.lookupOtherUserTransaction();
            Assert.fail();
        } catch (NamingException expected) {

        }
    }


}
