/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.exceptions;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * AS7-194
 * <p/>
 * Tests that application exceptions are handled correctly in the presence of CDI interceptors
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class CDIEJBInterceptorExceptionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(CDIEJBInterceptorExceptionTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans><interceptors><class>"+ExceptionInterceptor.class.getName() + "</class></interceptors></beans>"),  "beans.xml");
        return jar;
    }

    @Inject
    private ExceptionBean bean;

    @Test
    public void testUncheckedException() {
        try {
            bean.unchecked();
            Assert.fail("expected exception");
        } catch (UncheckedException expected) {

        }
    }

    @Test
    public void testCheckedException() {
        try {
            bean.checked();
            Assert.fail("expected exception");
        } catch (SimpleApplicationException expected) {

        }
    }
}
