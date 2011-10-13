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
package org.jboss.as.test.integration.weld.ejb.packaging.warlib;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * AS7-1089
 *
 * Tests the EJB's in WAR/lib are looked up using the correct bean manager
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbInWarLibPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {
         WebArchive war = ShrinkWrap.create(WebArchive.class, "CdiInterceptorPackaging.war");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(WarLibEjb.class, WarLibInterface.class);
        lib.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        war.addAsLibrary(lib);

        war.addClasses(EjbInWarLibPackagingTestCase.class, OtherEjb.class);

        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    @Test
    public void testEjbInWarLibResolvedCorrectly() throws NamingException {
        WarLibInterface cdiEjb = (WarLibInterface) new InitialContext().lookup("java:module/WarLibEjb");
        Assert.assertEquals("Hello", cdiEjb.getEjb().sayHello());
    }

}
