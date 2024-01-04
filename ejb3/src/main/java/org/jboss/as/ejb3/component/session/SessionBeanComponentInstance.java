/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.context.SessionContextImpl;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponentInstance extends EjbComponentInstance {
    private static final long serialVersionUID = 5176535401148504579L;

    private transient volatile SessionContextImpl sessionContext;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected SessionBeanComponentInstance(final BasicComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    public SessionBeanComponent getComponent() {
        return (SessionBeanComponent) super.getComponent();
    }

    protected abstract SessionID getId();

    @Override
    public SessionContextImpl getEjbContext() {
        if (sessionContext == null) {
            synchronized (this) {
                if (sessionContext == null) {
                    this.sessionContext = new SessionContextImpl(this);
                }
            }
        }
        return sessionContext;
    }
}
