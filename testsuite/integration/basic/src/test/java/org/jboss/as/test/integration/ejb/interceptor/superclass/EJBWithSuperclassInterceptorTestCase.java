/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.superclass;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;


@ExtendWith(ArquillianExtension.class)
public class EJBWithSuperclassInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "interceptor-descriptor-test.jar");
        jar.addPackage(EJBWithSuperclassInterceptorTestCase.class.getPackage());
        jar.addAsManifestResource(EJBWithSuperclassInterceptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void test() throws Exception {
        Foo foo = (Foo) new InitialContext().lookup("java:module/" + FooBean.class.getSimpleName()
                + "!" + Foo.class.getName());
        FooInterceptor.invoked = false;
        foo.foo("foo");
        Assertions.assertTrue(FooInterceptor.invoked);
    }
}
