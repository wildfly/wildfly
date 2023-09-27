/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
