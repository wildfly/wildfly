/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
