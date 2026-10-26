/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that context data from the server is returned to client interceptors
 * for an in-vm invocation.
 *
 * @author Stuart Douglas
 * @author Brad Maxwell
 */
@ExtendWith(ArquillianExtension.class)
public class ClientInterceptorReturnDataLocalTestCase {

    @Deployment()
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ClientInterceptorReturnDataLocalTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(ClientInterceptorReturnDataLocalTestCase.class.getPackage());
        jar.addAsServiceProvider(EJBClientInterceptor.class, ClientInterceptor.class);
        return jar;
    }

    @EJB
    TestRemote testSingleton;

    @Test
    public void testInvokeWithClientInterceptorData() {
        try {
            UseCaseValidator useCaseValidator = new UseCaseValidator(UseCaseValidator.Interface.LOCAL);
            testSingleton.invoke(useCaseValidator);
        } catch(TestException te) {
            Assertions.fail(te.getMessage());
        }
    }
}
