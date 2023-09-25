/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * Tests that ejb's can be injected into an injection point of a type that does not exactly correlate
 * to one of their views.
 *
 * WELD-921
 * Also tests that EJB invocations on base class methods work
 *
 */
@RunWith(Arquillian.class)
public class EjbInheritanceTestCase {
    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "inheritance-test.war")
            .addPackage(EjbInheritanceTestCase.class.getPackage())
            .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @Inject
    private Synchronizations sync;

    @Inject
    private InjectingCDIBean injectingCDIBean;

    @Test
    public void testEjbCanBeInjectedViaNonViewInjectionPoint() {
        sync.register();
    }

    @Test
    public void testCdiInjectedEjb() {
        Assert.assertEquals("Hello", injectingCDIBean.sayHello());
        Assert.assertEquals("Goodbye", injectingCDIBean.sayGoodbye());
        Assert.assertEquals("Interface", injectingCDIBean.callInterfaceMethod());
    }


}
