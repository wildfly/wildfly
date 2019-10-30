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
package org.jboss.as.test.integration.weld.extensions;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * AS7-623
 *
 * Make sure extensions in WEB-INF/lib of a war in an ear are discovered.
 *
 * A jar with a portable extension that adds MyBean as a bean is added to a war that is deployed as an ear
 *
 * Normally MyBean would not be a bean as it is not in a jar with a beans.xml
 *
 * The test checks that it is possible to inject MyBean
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ExtensionsInEarDiscoveredTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testExtensions.ear");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war");
        JavaArchive warLib = ShrinkWrap.create(JavaArchive.class, "testLib.jar");

        warLib.addClasses(MyBean.class, AddBeanExtension.class);
        warLib.add(new StringAsset(AddBeanExtension.class.getName()), "META-INF/services/" + Extension.class.getName());

        war.addAsLibrary(warLib);
        war.addClass(WarSLSB.class);
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(ExtensionsInEarDiscoveredTestCase.class, SomeInterface.class);
        ear.addAsLibrary(lib);

        ear.addAsModule(war);

        return ear;

    }


    @Test
    public void testExtensionIsLoaded() throws NamingException {
        SomeInterface bean = (SomeInterface) new InitialContext().lookup("java:global/testExtensions/testWar/WarSLSB");
        bean.testInjectionWorked();
    }

}
