/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.AbstractComponentInstance;
import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponentInstance extends AbstractComponentInstance {
    private BaseSessionContext sessionContext;

    /**
     * Construct a new instance.
     *
     * @param component the component
     * @param instance  the object instance
     */
    protected SessionBeanComponentInstance(final SessionBeanComponent component, final Object instance, final List<Interceptor> preDestroyInterceptors, final InterceptorFactoryContext factoryContext) {
        super(component, instance, preDestroyInterceptors, factoryContext);

        this.sessionContext = new BaseSessionContext(component, instance);
    }

    protected SessionContext getSessionContext() {
        return sessionContext;
    }
}
