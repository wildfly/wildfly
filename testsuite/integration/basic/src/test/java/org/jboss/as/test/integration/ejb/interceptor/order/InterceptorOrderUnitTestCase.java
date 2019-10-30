/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.interceptor.order;

import javax.naming.InitialContext;

import org.junit.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Test migrated from EJB3 testsuite [JBQA-5451] - from test ejbthree1852
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class InterceptorOrderUnitTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "interceptor-descriptor-test.jar");
        jar.addPackage(InterceptorOrderUnitTestCase.class.getPackage());
        jar.addAsManifestResource(InterceptorOrderUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void test() throws Exception {
        GreeterRemote greeter = (GreeterRemote) new InitialContext().lookup("java:module/" + GreeterBean.class.getSimpleName()
                + "!" + GreeterRemote.class.getName());

        String result = greeter.sayHi("ejbthree1852");
        Assert.assertEquals("SecondFirstHi ejbthree1852", result);
    }
}
