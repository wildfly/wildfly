/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.context.EJBContextImpl;
import org.jboss.invocation.Interceptor;
/**
 * @author Stuart Douglas
 */
public abstract class EjbComponentInstance extends BasicComponentInstance {

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected EjbComponentInstance(final BasicComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    public EJBComponent getComponent() {
        return (EJBComponent) super.getComponent();
    }

    public abstract EJBContextImpl getEjbContext();
}
