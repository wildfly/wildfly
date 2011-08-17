/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.TimedComponentInstance;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.ejb.Timer;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponentInstance extends SessionBeanComponentInstance implements TimedComponentInstance {

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private final Map<Method, Interceptor> timeoutInterceptors;

    /**
     * Construct a new instance.
     *
     * @param component           the component
     * @param timeoutInterceptors
     */
    protected StatelessSessionComponentInstance(final BasicComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final Map<Method, Interceptor> timeoutInterceptors) {
        super(component, instanceReference, preDestroyInterceptor, methodInterceptors);
        this.timeoutInterceptors = timeoutInterceptors;
    }

    @Override
    protected Serializable getId() {
        return null;
    }

    @Override
    public void invokeTimeoutMethod(final Method method, final Timer timer) {
        final Interceptor interceptor = timeoutInterceptors.get(method);
        if (interceptor == null) {
            throw new RuntimeException("Unknown timeout method " + method);
        }
        try {
            InterceptorContext context = prepareInterceptorContext();
            context.setMethod(method);
            context.setTimer(timer);
            context.setTarget(getInstance());
            final Object[] params;
            if (method.getParameterTypes().length == 1) {
                params = new Object[1];
                params[0] = timer;
            } else {
                params = EMPTY_OBJECT_ARRAY;
            }
            context.setParameters(params);
            interceptor.processInvocation(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void invokeTimeoutMethod(final Timer timer) {
        final StatelessSessionComponent component = (StatelessSessionComponent) getComponent();
        final Method method = component.getTimeoutMethod();
        if (method == null) {
            throw new IllegalArgumentException("Component " + component.getComponentName() + " does not have a timeout method");
        }
        invokeTimeoutMethod(method, timer);
    }
}
