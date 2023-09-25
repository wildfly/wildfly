/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.injection;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * Tests that EJB interceptors can use CDI injection.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class CDIInjectionIntoEJBInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(CDIInjectionIntoEJBInterceptorTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return jar;
    }

    @Inject
    private MessageBean bean;

    @Test
    public void testCDIInjectionIntoEJBInterceptor() {
        Assert.assertEquals("Hello World", bean.getMessage());
    }
}
