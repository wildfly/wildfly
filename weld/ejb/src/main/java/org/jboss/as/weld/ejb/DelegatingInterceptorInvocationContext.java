/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.ejb;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.weld.exceptions.WeldException;

public class DelegatingInterceptorInvocationContext implements InvocationContext {

    private InvocationContext delegateInvocationContext;

    private final List<Interceptor> invocationQueue;
    private final List<Object> interceptorInstances;
    private int position;

    private InterceptionType interceptionType;

    public DelegatingInterceptorInvocationContext(InvocationContext delegateInvocationContext, List<Interceptor<?>> interceptors, List<Object> instances, InterceptionType interceptionType) {
        this.delegateInvocationContext = delegateInvocationContext;
        this.interceptionType = interceptionType;
        this.invocationQueue = new ArrayList<Interceptor>(interceptors);
        this.interceptorInstances = new ArrayList<Object>(instances);
        position = 0;
    }

    public Map<String, Object> getContextData() {
        return delegateInvocationContext.getContextData();
    }

    public Method getMethod() {
        return delegateInvocationContext.getMethod();
    }

    @Override
    public Constructor<?> getConstructor() {
        return delegateInvocationContext.getConstructor();
    }

    public Object[] getParameters() {
        return delegateInvocationContext.getParameters();
    }

    public Object getTarget() {
        return delegateInvocationContext.getTarget();
    }

    public Object proceed() throws Exception {
        int oldPosition = position;
        try {
            if (position < invocationQueue.size()) {
                Object interceptorInstance = interceptorInstances.get(position);
                try {
                    return invocationQueue.get(position++).intercept(interceptionType, interceptorInstance, this);
                } catch (Exception e) {
                    // Unwrap WeldException
                    if (e instanceof WeldException && e.getCause() instanceof Exception) {
                        throw ((Exception) e.getCause());
                    } else {
                        throw e;
                    }
                }
            } else {
                return delegateInvocationContext.proceed();
            }
        } finally {
            position = oldPosition;
        }
    }

    public void setParameters(Object[] params) {
        delegateInvocationContext.setParameters(params);
    }

    public Object getTimer() {
        return delegateInvocationContext.getTimer();
    }
}