/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.context;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * A simple test that CDI interceptors are applied to EJB's, and that they are
 * applied in the correct order.
 *
 * @author baranowb
 * @author Brad Maxwell
 */
@ExtendWith(ArquillianExtension.class)
public class WeldInterceptorEJBContextTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(WeldInterceptorEJBContextTestCase.class.getPackage());
        jar.addAsManifestResource(
                new StringAsset("<beans><interceptors><class>" + ServiceLoggedInterceptor.class.getName() + "</class></interceptors></beans>"),
                "beans.xml");
        return jar;
    }

    @Inject
    private TestSingleton bean;

    @Test
    public void testEJBContextInjection() {
        //deployment is enough
        Assertions.assertTrue(bean.test(), "Call failed?");
    }
}
