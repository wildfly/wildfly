/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that lifecycle interceptors are handed correctly,
 * as per the interceptors specification.
 *
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
public class PreDestroyInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,"testpredestroy.jar");
        jar.addPackage(PreDestroyInterceptorTestCase.class.getPackage());
        jar.addAsManifestResource(PreDestroyInterceptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testPreDestroyInterceptor() throws NamingException {

        InitialContext ctx = new InitialContext();
        PreDestroySFSB bean = (PreDestroySFSB)ctx.lookup("java:module/" + PreDestroySFSB.class.getSimpleName());
        Assertions.assertTrue(PreDestroySFSB.postConstructCalled);
        Assertions.assertTrue(PreDestroyInterceptor.postConstruct);
        Assertions.assertFalse(PreDestroyInterceptor.postConstructInvocationTargetNull, "InvocationContext.getTarget() was null for post-construct interceptor");
        Assertions.assertTrue(PreDestroyInterceptorDescriptor.postConstruct);
        Assertions.assertFalse(PreDestroyInterceptorDescriptor.postConstructInvocationTargetNull, "InvocationContext.getTarget() was null for post-construct interceptor");

        bean.remove();
        Assertions.assertTrue(PreDestroySFSB.preDestroyCalled);
        Assertions.assertTrue(PreDestroyInterceptor.preDestroy);
        Assertions.assertFalse(PreDestroyInterceptor.preDestroyInvocationTargetNull, "InvocationContext.getTarget() was null for pre-destroy interceptor");
        Assertions.assertTrue(PreDestroyInterceptorDescriptor.preDestroy);
        Assertions.assertFalse(PreDestroyInterceptorDescriptor.preDestroyInvocationTargetNull, "InvocationContext.getTarget() was null for pre-destroy interceptor");
    }
}
