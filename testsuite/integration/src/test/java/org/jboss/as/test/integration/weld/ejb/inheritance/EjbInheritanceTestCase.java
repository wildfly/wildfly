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
package org.jboss.as.test.integration.weld.ejb.inheritance;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

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
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
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
