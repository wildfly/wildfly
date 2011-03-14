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

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.base.BaseSessionContext;
import org.jboss.ejb3.context.base.BaseSessionInvocationContext;
import org.jboss.ejb3.context.spi.SessionContext;
import org.jboss.invocation.Interceptor;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class SessionBeanComponent extends EJBComponent implements org.jboss.ejb3.context.spi.SessionBeanComponent {
    /**
     * Construct a new instance.
     *
     * @param configuration the component configuration
     */
    protected SessionBeanComponent(final SessionBeanComponentConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected List<Interceptor> applyInjections(Object instance) {
        // TODO: a temporary hack until injection interceptors are in place
        BaseSessionInvocationContext invocationContext = new BaseSessionInvocationContext(null, null, null) {
            @Override
            public Object proceed() throws Exception {
                throw new RuntimeException("Do not call proceed");
            }
        };
        BaseSessionContext instanceContext = new BaseSessionContext(this, instance);
        invocationContext.setEJBContext(instanceContext);
        CurrentInvocationContext.push(invocationContext);
        try {
            return super.applyInjections(instance);
        }
        finally {
            CurrentInvocationContext.pop();
        }
    }

    @Override
    public <T> T getBusinessObject(SessionContext ctx, Class<T> businessInterface) throws IllegalStateException {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.session.SessionBeanComponent.getBusinessObject");
    }

    @Override
    public EJBLocalObject getEJBLocalObject(SessionContext ctx) throws IllegalStateException {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.session.SessionBeanComponent.getEJBLocalObject");
    }

    @Override
    public EJBObject getEJBObject(SessionContext ctx) throws IllegalStateException {
        throw new RuntimeException("NYI: org.jboss.as.ejb3.component.session.SessionBeanComponent.getEJBObject");
    }
}
