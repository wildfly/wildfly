/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
