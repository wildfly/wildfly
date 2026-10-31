/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.interceptors.exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@ExtendWith(ArquillianExtension.class)
public class ExceptionsFromInterceptorsTestCase {
    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "interceptors-exceptions.jar")
            .addPackage(ExceptionsFromInterceptorsTestCase.class.getPackage());
    }

    private static <T> T lookup(final String name, final Class<T> cls) throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            return cls.cast(ctx.lookup(name));
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testUndeclared() throws Exception {
        try {
            lookup("java:global/interceptors-exceptions/PitcherBean", PitcherBean.class).fastball();
            fail("Should have thrown a (Runtime)Exception");
        }
        catch (Exception e) {
            assertTrue(e instanceof RuntimeException, "Did not declare exception - " + e);
        }
    }
}
