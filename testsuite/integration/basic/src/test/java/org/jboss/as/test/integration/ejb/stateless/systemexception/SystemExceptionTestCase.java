/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateless.systemexception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that SLSB's are not re-used after a system exception
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SystemExceptionTestCase {

    private static final String ARCHIVE_NAME = "SystemExceptionTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(SystemExceptionTestCase.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    /**
     * Ensure that a system exception destroys the bean.
     *
     * @throws Exception
     */
    @Test
    public void testSystemExceptionDestroysBean() throws Exception {

        SystemExceptionSLSB slsb = lookup(SystemExceptionSLSB.class);
        for (int i = 1; i < 50; ++i) {
            boolean fail = false;
            try {
                slsb.systemException();
                fail = true;
            } catch (RuntimeException e) {

            }
            if(fail) {
                Assert.fail("No system exception was thrown, which means bean has been re-used after a system exception");
            }
        }
    }
}
