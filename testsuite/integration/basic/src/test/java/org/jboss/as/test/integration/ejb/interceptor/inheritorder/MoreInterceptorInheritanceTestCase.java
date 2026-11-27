/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import javax.naming.InitialContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Interceptor inheritance test.
 *
 * @author Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(supposedResult, bean.run());
    }
}
