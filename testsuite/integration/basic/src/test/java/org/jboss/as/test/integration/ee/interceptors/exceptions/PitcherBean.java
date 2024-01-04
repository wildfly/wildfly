/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.interceptors.exceptions;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
public class PitcherBean {
    @Interceptors(ThrowingClassCastExceptionInterceptor.class)
    public void curveball() {
        // do nothing
    }

    @Interceptors(ThrowingUndeclaredExceptionInterceptor.class)
    public void fastball() {
        // do nothing
    }
}
