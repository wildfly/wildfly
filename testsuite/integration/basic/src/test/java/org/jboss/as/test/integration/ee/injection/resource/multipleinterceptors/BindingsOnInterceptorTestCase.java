/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.multipleinterceptors;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that @Resource bindings on interceptors that are applied to multiple
 * components without their own naming context work properly, and do not try
 * and create two duplicate bindings in the same namespace.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class BindingsOnInterceptorTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multiple-bindings-interceptors.war");
        war.addClasses(Bean1.class, Bean2.class, BindingsOnInterceptorTestCase.class, MyInterceptor.class, SimpleStatelessBean.class);
        return war;
    }

    @Test
    public void testCorrectBinding() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/" + MyInterceptor.class.getName() + "/simpleStatelessBean");
        Assert.assertTrue(result instanceof SimpleStatelessBean);
    }

}
