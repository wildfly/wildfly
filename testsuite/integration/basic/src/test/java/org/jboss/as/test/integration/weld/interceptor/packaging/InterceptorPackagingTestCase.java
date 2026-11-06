/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.interceptor.packaging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;

/**
 *
 * Tests that interceptors that are packaged in separate jar files.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class InterceptorPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "interceptortest.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mod1.jar");
        jar.addClasses(InterceptedBean.class, SimpleEjb.class, InterceptorPackagingTestCase.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"><interceptors><class>" + SimpleInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        ear.addAsModule(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "mod2.jar");
        jar.addClasses(SimpleInterceptor.class, SimpleEjb2.class, Intercepted.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"><interceptors><class>" + SimpleInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        ear.addAsModule(jar);

        return ear;
    }

    @Inject
    private SimpleEjb simpleEjb;

    @Inject
    private SimpleEjb2 simpleEjb2;

    @Inject
    private InterceptedBean interceptedBean;

    @Test
    public void testInterceptorEnabled() {
        Assertions.assertEquals("Hello World", simpleEjb2.sayHello());
        Assertions.assertEquals("Hello World", interceptedBean.sayHello());
        Assertions.assertEquals("Hello World", simpleEjb.sayHello());
    }

    @Test
    public void testPostConstructInvoked() {
        Assertions.assertEquals(SimpleInterceptor.POST_CONSTRUCT_MESSAGE  + " World", simpleEjb2.getPostConstructMessage());
    }


}
