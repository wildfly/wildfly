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

package org.jboss.as.weld.interceptors;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.as.weld.spi.InterceptorInstances;
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
    private final ComponentInterceptorSupport interceptorSupport;

    private Jsr299BindingsInterceptor(InterceptionType interceptionType, ComponentInterceptorSupport interceptorSupport) {
        this.interceptionType = interceptionType;
        this.interceptorSupport = interceptorSupport;
    }

    public static InterceptorFactory factory(final InterceptionType interceptionType, final ServiceBuilder<?> builder, final ServiceName interceptorBindingServiceName, final ComponentInterceptorSupport interceptorSupport) {
        Jsr299BindingsInterceptor interceptor = new Jsr299BindingsInterceptor(interceptionType, interceptorSupport);
        builder.addDependency(interceptorBindingServiceName, InterceptorBindings.class, interceptor.interceptorBindings);
        return new ImmediateInterceptorFactory(interceptor);
    }

    protected Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors, InterceptorInstances interceptorInstances)
            throws Exception {
        List<Object> currentInterceptorInstances = new ArrayList<Object>();
        for (Interceptor<?> interceptor : currentInterceptors) {
            currentInterceptorInstances.add(interceptorInstances.getInstances().get(interceptor.getBeanClass().getName()).getInstance());
        }
        if (currentInterceptorInstances.size() > 0) {
            return interceptorSupport.delegateInterception(invocationContext, interceptionType, currentInterceptors, currentInterceptorInstances);
        } else {
            return invocationContext.proceed();
        }

    }


    private Object doMethodInterception(InvocationContext invocationContext, InterceptionType interceptionType, InterceptorInstances interceptorInstances, InterceptorBindings interceptorBindings)
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
        final InterceptorInstances interceptorInstances = interceptorSupport.getInterceptorInstances(componentInstance);
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

    private Object doLifecycleInterception(final InterceptorContext context, InterceptorInstances interceptorInstances, final InterceptorBindings interceptorBindings) throws Exception {
        if (interceptorBindings == null) {
            return context.proceed();
        } else {
            List<Interceptor<?>> currentInterceptors = interceptorBindings.getLifecycleInterceptors(interceptionType);
            return delegateInterception(context.getInvocationContext(), interceptionType, currentInterceptors, interceptorInstances);
        }
    }
}
