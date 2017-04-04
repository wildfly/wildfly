/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.weld.ejb;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

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