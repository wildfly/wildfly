/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.interceptor.exception;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that exception thrown in interceptor method intercepting a session bean does not get suppressed.
 *
 * @author Matus Abaffy
 * @author Jozef Hartinger
 */
@RunWith(Arquillian.class)
public class EjbInterceptorExceptionTestCase {

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(EjbInterceptorExceptionTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Inject
    Instance<Baf> bafInstance;

    @Inject
    Instance<Bar> barInstance;

    @Inject
    Instance<Foo> fooInstance;

    @Test
    public void testExceptionNotSuppressedInAroundConstructCallback() {
        assertNotNull(bafInstance);
        BarPostConstructInterceptor.reset();
        boolean fail = false;
        try {
            bafInstance.get().doSomething();
            fail = true;
        } catch (Throwable e) {
            // OK
        }
        if (fail) {
            fail("Assertion error in AroundConstruct interceptor method was suppressed.");
        }
        assertTrue(BafAroundConstructInterceptor.isAroundConstructCalled());
    }

    @Test
    public void testExceptionNotSuppressedInPostConstructCallback() {
        assertNotNull(barInstance);
        BarPostConstructInterceptor.reset();
        try {
            barInstance.get().doSomething();
            fail("Exception in PostConstruct interceptor method was suppressed.");
        } catch (Exception e) {
            // OK
        }
        assertTrue(BarPostConstructInterceptor.isPostConstructCalled());
    }

    @Test
    public void testExceptionNotSuppressedInAroundInvoke() {
        assertNotNull(fooInstance);
        FooAroundInvokeInterceptor.reset();
        try {
            Foo foo = fooInstance.get();
            foo.doSomething();
            fail("Exception in AroundInvoke interceptor method was suppressed.");
        } catch (Exception e) {
            // OK
        }
        assertTrue(FooAroundInvokeInterceptor.isAroundInvokeCalled());
    }
}
