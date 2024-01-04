/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * SimpleStatelessBean
 *
 * @author Jaikiran Pai
 */
@Stateless
@Interceptors(SimpleInterceptor.class)
@Remote(InjectionTester.class)
public class SimpleStatelessBean implements InjectionTester {
    @PersistenceContext(unitName = "interceptors-test")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    public void assertAllInjectionsDone() throws IllegalStateException {
        if (em == null) {
            throw new IllegalStateException("EntityManager was *not* injected in bean " + this.getClass().getName());
        }

        if (this.sessionContext == null) {
            throw new IllegalStateException("SessionContext was *not* injected in interceptor "
                    + this.getClass().getName());
        }
    }

}
