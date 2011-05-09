/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.weld.ejb;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DelegatingInterceptorInvocationContext implements InvocationContext {

    private InvocationContext delegateInvocationContext;

    private List<Interceptor> invocationQueue;
    private List<Object> interceptorInstances;
    int position;

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
                return invocationQueue.get(position++).intercept(interceptionType, interceptorInstance, this);
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
        throw new UnsupportedOperationException("Get timer not supported");
    }
}