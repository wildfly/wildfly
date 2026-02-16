/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static org.junit.Assert.fail;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ContextService;
/**
 * The bean that will do the actual test.
 * @author emartins
 */
@Stateless
public class ContextualProxyExceptionUnwrapBean {

    static class UndeclaredException extends RuntimeException {
    }

    @Resource
    private ContextService contextService;

    public void test() throws Exception {
        try {
            contextService.createContextualProxy(this::throwDeclaredException, ContextualProxy.class).test();
            fail("Declared exception did not propagate");
        } catch (ContextualProxy.DeclaredException ex) {
            // expected
        }
        try {
            contextService.createContextualProxy(this::throwUndeclaredException, ContextualProxy.class).test();
            fail("Undeclared exception did not propagate");
        } catch (UndeclaredException ex) {
            // expected
        }
    }

    private void throwDeclaredException() throws ContextualProxy.DeclaredException {
        throw new ContextualProxy.DeclaredException();
    }

    private void throwUndeclaredException() throws ContextualProxy.DeclaredException {
        throw new UndeclaredException();
    }
}
