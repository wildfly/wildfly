/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.enterprise.inject.spi.Extension;
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
