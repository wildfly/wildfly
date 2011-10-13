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
package org.jboss.as.test.integration.ee.interceptors.exceptions;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
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
            assertTrue("Did not declare exception - " + e, e instanceof RuntimeException);
        }
    }
}
