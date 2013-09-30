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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.stateful.SerializedCdiInterceptorsKey;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.ejb.spi.InterceptorBindings;

/**
 * Interceptor for applying the JSR-299 specific interceptor bindings.
 * <p/>
 * It is a separate interceptor, as it needs to be applied after all
 * the other existing interceptors.
 *
 * @author Marius Bogoevici
 * @author Stuart Douglas
 */
public class Jsr299BindingsInterceptor implements org.jboss.invocation.Interceptor {

    private final InterceptionType interceptionType;
    private final InjectedValue<InterceptorBindings> interceptorBindings = new InjectedValue<InterceptorBindings>();

    private Jsr299BindingsInterceptor(InterceptionType interceptionType) {
        this.interceptionType = interceptionType;
    }

    public static InterceptorFactory factory(final InterceptionType interceptionType, final ServiceBuilder<?> builder, final ServiceName interceptorBindingServiceName) {
        Jsr299BindingsInterceptor interceptor = new Jsr299BindingsInterceptor(interceptionType);
        builder.addDependency(interceptorBindingServiceName, InterceptorBindings.class, interceptor.interceptorBindings);
        return new ImmediateInterceptorFactory(interceptor);
    }

    protected Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors, WeldInterceptorInstances interceptorInstances)
            throws Exception {
        List<Object> currentInterceptorInstances = new ArrayList<Object>();
        for (Interceptor<?> interceptor : currentInterceptors) {
            currentInterceptorInstances.add(interceptorInstances.getInterceptorInstances().get(interceptor.getBeanClass().getName()).getInstance());
        }
        if (currentInterceptorInstances.size() > 0) {
            return new DelegatingInterceptorInvocationContext(invocationContext, currentInterceptors, currentInterceptorInstances, interceptionType).proceed();
        } else {
            return invocationContext.proceed();
        }

    }


    private Object doMethodInterception(InvocationContext invocationContext, InterceptionType interceptionType, WeldInterceptorInstances interceptorInstances, InterceptorBindings interceptorBindings)
            throws Exception {
        if (interceptorBindings != null) {
            List<Interceptor<?>> currentInterceptors = interceptorBindings.getMethodInterceptors(interceptionType, invocationContext.getMethod());
            return delegateInterception(invocationContext, interceptionType, currentInterceptors, interceptorInstances);
        } else {
            return invocationContext.proceed();
        }
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        final WeldInterceptorInstances interceptorInstances = (WeldInterceptorInstances) componentInstance.getInstanceData(SerializedCdiInterceptorsKey.class);
        final InterceptorBindings interceptorBindings = this.interceptorBindings.getValue();
        switch (interceptionType) {
            case AROUND_INVOKE:
                return doMethodInterception(context.getInvocationContext(), InterceptionType.AROUND_INVOKE, interceptorInstances, interceptorBindings);
            case AROUND_TIMEOUT:
                return doMethodInterception(context.getInvocationContext(), InterceptionType.AROUND_TIMEOUT, interceptorInstances, interceptorBindings);
            case PRE_DESTROY:
                try {
                    return doLifecycleInterception(context, interceptorInstances, interceptorBindings);
                } finally {
                    interceptorInstances.getCreationalContext().release();
                }
            case POST_CONSTRUCT:
                return doLifecycleInterception(context, interceptorInstances, interceptorBindings);
            case AROUND_CONSTRUCT:
                return doLifecycleInterception(context, interceptorInstances, interceptorBindings);
            default:
                //should never happen
                return context.proceed();
        }
    }

    private Object doLifecycleInterception(final InterceptorContext context, WeldInterceptorInstances interceptorInstances, final InterceptorBindings interceptorBindings) throws Exception {
        try {
            if (interceptorBindings != null) {
                List<Interceptor<?>> currentInterceptors = interceptorBindings.getLifecycleInterceptors(interceptionType);
                delegateInterception(context.getInvocationContext(), interceptionType, currentInterceptors, interceptorInstances);
            }
        } finally {
            return context.proceed();
        }
    }
}
