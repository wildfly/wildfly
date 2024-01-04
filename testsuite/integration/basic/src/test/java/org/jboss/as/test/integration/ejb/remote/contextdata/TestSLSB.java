/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

@Stateless
@Interceptors(EjbInterceptor.class)
public class TestSLSB implements TestRemote {

    @Resource
    SessionContext sessionContext;

    @Override
    public UseCaseValidator invoke(UseCaseValidator useCaseValidator) throws TestException {

        // test when the ejb is invoked
        useCaseValidator.test(UseCaseValidator.InvocationPhase.SERVER_EJB_INVOKE, sessionContext.getContextData());
        return useCaseValidator;
    }
}
