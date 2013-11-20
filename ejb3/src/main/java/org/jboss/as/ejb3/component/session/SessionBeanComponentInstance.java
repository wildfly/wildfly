/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
