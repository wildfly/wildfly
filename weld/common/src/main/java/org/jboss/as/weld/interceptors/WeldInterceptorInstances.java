/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.interceptors;

import java.io.Serializable;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Interceptor;

import org.jboss.as.weld.spi.InterceptorInstances;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
* @author Stuart Douglas
*/
public class WeldInterceptorInstances implements InterceptorInstances, Serializable {
    private static final long serialVersionUID = 1L;
    private final CreationalContext<Object> creationalContext;
    private final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;

    public WeldInterceptorInstances(final CreationalContext<Object> creationalContext, final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances) {
        this.creationalContext = creationalContext;
        this.interceptorInstances = interceptorInstances;
    }

    @Override
    public CreationalContext<Object> getCreationalContext() {
        return creationalContext;
    }

    @Override
    public Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> getInstances() {
        return interceptorInstances;
    }
}
