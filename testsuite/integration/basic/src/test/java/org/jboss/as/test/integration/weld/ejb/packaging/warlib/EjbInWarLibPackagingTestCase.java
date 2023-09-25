/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
