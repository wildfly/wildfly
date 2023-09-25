/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.interceptor.Interceptors;

/**
 * @author Paul Ferraro
 */
@Stateful
@Interceptors(IncrementorInterceptor.class)
@Intercepted
public class NestedIncrementorBean implements Incrementor {

    @EJB
    private Nested nested;

    @Override
    public int increment() {
        return this.nested.increment();
    }
}
