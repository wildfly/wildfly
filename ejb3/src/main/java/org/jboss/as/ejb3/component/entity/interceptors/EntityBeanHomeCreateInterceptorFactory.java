/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.component.entity.interceptors;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.ejb.CreateException;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Interceptor that hooks up create methods for Entity beans.
 * <p/>
 * This is a view level interceptor that should be attached to create methods on the home interface.
 * <p/>
 * It simply creates an instance of the object view, passing in the creation parameters. The object view post create
 * interceptors are responsible for getting an instance and associating it with the identity.
 *
 * @author Stuart Douglas
 */
public class EntityBeanHomeCreateInterceptorFactory implements InterceptorFactory {

    private final InjectedValue<ComponentView> viewToCreate = new InjectedValue<ComponentView>();

    public static final Object EJB_CREATE_METHOD_KEY = new Object();
    public static final Object EJB_POST_CREATE_METHOD_KEY = new Object();

    public static final Object PARAMETERS_KEY = new Object();

    private final Method ejbCreate;

    private final Method ejbPostCreate;

    public EntityBeanHomeCreateInterceptorFactory(final Method method, final Method ejbPostCreate) {
        this.ejbCreate = method;
        this.ejbPostCreate = ejbPostCreate;
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final ComponentView view = viewToCreate.getValue();
                final HashMap<Object, Object> ctx = new HashMap<Object, Object>();
                ctx.put(EJB_CREATE_METHOD_KEY, ejbCreate);
                ctx.put(EJB_POST_CREATE_METHOD_KEY, ejbPostCreate);
                ctx.put(PARAMETERS_KEY, context.getParameters());
                try {
                    final ManagedReference instance = view.createInstance(ctx);
                    return instance.getInstance();
                } catch (RuntimeException e) {
                    //throw the correct exception type
                    Throwable cause = e.getCause();
                    if (cause instanceof CreateException) {
                        throw (CreateException) cause;
                    }
                    throw e;
                }
            }
        };
    }

    public InjectedValue<ComponentView> getViewToCreate() {
        return viewToCreate;
    }
}
