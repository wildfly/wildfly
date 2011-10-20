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

import java.rmi.RemoteException;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.NoSuchEJBException;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

/**
 * Interceptor factory for entity beans that associates an invocation with a primary key
 *
 * @author Stuart Douglas
 */
public class EntityBeanAssociatingInterceptorFactory implements InterceptorFactory {

    private final Logger log = Logger.getLogger(EntityBeanAssociatingInterceptorFactory.class);

    public static final EntityBeanAssociatingInterceptorFactory INSTANCE = new EntityBeanAssociatingInterceptorFactory();

    private EntityBeanAssociatingInterceptorFactory() {
    }

    @Override
    public Interceptor create(final  InterceptorFactoryContext factoryContext) {

        return new AbstractEJBInterceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final EntityBeanComponent component = getComponent(context, EntityBeanComponent.class);

                final Object primaryKey = context.getPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);
                if (primaryKey == null) {
                    throw new NoSuchEJBException("Invocation was not associated with an instance, primary key was null, instance may have been removed");
                }

                final EntityBeanComponentInstance instance = component.getCache().get(primaryKey);

                if(instance.isRemoved()) {
                    throw new NoSuchEJBException("Instance of " + component.getComponentName() + " with primary key " + primaryKey + " has been removed");
                }

                try {
                    context.putPrivateData(ComponentInstance.class, instance);
                    return context.proceed();
                } catch (Exception ex) {
                    // Detect app exception
                    if (component.getApplicationException(ex.getClass(), context.getMethod()) != null) {
                        // it's an application exception, just throw it back.
                        throw ex;
                    }
                    if (ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                        throw ex;
                    }
                    if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                        if (log.isTraceEnabled())
                            log.trace("Discarding bean " + primaryKey + " because of exception", ex);
                        component.getCache().discard(instance);
                    }
                    throw ex;
                } catch (final Error e) {
                    if (log.isTraceEnabled())
                        log.trace("Discarding bean " + primaryKey + " because of error", e);
                    component.getCache().discard(instance);
                    throw e;
                } catch (final Throwable t) {
                    if (log.isTraceEnabled())
                        log.trace("Discarding bean " + primaryKey + " because of Throwable", t);
                    component.getCache().discard(instance);
                    throw new RuntimeException(t);
                } finally {
                    // the StatefulSessionSynchronizationInterceptor will take care of releasing
                    context.putPrivateData(ComponentInstance.class, null);
                }
            }
        };

    }
}
