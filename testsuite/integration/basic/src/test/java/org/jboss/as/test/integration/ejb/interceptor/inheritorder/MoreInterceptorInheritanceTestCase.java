/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import javax.naming.InitialContext;

import org.junit.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Interceptor inheritance test.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class MoreInterceptorInheritanceTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "more-interceptor-test.jar");
        jar.addPackage(MoreInterceptorInheritanceTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void test() throws Exception {
        CClass bean = (CClass) ctx.lookup("java:module/" + CClass.class.getSimpleName());

        String supposedResult = "A1 A2 CGrandparent CChild " + // Class interceptor
                "Cmethod " + BClass.class.getSimpleName() + ".method " + CClass.class.getSimpleName() + ".method " + // Method
                                                                                                                     // interceptors
                AClass.class.getSimpleName() + BClass.class.getSimpleName() + CClass.class.getSimpleName(); // Class method
                                                                                                            // calls
        Assert.assertEquals(supposedResult, bean.run());
    }
}
