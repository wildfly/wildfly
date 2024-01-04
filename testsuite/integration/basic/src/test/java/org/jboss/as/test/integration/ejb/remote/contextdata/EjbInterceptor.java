/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import org.junit.Assert;

public class EjbInterceptor {

    @AroundInvoke
    public Object interceptor(InvocationContext context) throws Exception {
        Assert.assertNotNull(context.getParameters());
        Assert.assertEquals(1, context.getParameters().length);
        Assert.assertTrue(context.getParameters()[0] instanceof UseCaseValidator);

        // test before the ejb is invoked
        UseCaseValidator useCaseValidator = (UseCaseValidator) context.getParameters()[0];
        useCaseValidator.test(UseCaseValidator.InvocationPhase.SERVER_INT_BEFORE, context.getContextData());

        // test after the ejb is invoked
        useCaseValidator = (UseCaseValidator) context.proceed();
        useCaseValidator.test(UseCaseValidator.InvocationPhase.SERVER_INT_AFTER, context.getContextData());

        return useCaseValidator;
    }
}