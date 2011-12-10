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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Interceptor factory for entity beans that class the corresponding ejbCreate method.
 * <p/>
 * This is a post construct interceptor for the Ejb(Local)Object view
 *
 * @author Stuart Douglas
 */
public class EntityBeanEjbCreateMethodInterceptorFactory implements InterceptorFactory {

    public static final EntityBeanEjbCreateMethodInterceptorFactory INSTANCE = new EntityBeanEjbCreateMethodInterceptorFactory();

    protected EntityBeanEjbCreateMethodInterceptorFactory() {
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        final Object existing = context.getContextData().get(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);

        final AtomicReference<Object> primaryKeyReference = new AtomicReference<Object>();
        context.getContextData().put(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, primaryKeyReference);

        final Method ejbCreate = (Method) context.getContextData().get(EntityBeanHomeCreateInterceptorFactory.EJB_CREATE_METHOD_KEY);
        final Method ejbPostCreate = (Method) context.getContextData().get(EntityBeanHomeCreateInterceptorFactory.EJB_POST_CREATE_METHOD_KEY);
        final Object[] params = (Object[]) context.getContextData().get(EntityBeanHomeCreateInterceptorFactory.PARAMETERS_KEY);

        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {

                if (existing != null) {
                    primaryKeyReference.set(existing);
                    return existing;
                }
                final Component component = context.getPrivateData(Component.class);
                if (!(component instanceof EntityBeanComponent)) {
                    throw MESSAGES.unexpectedComponent(component, EntityBeanComponent.class);
                }
                final EntityBeanComponent entityBeanComponent = (EntityBeanComponent) component;
                //grab an unasociated entity bean from the pool
                final EntityBeanComponentInstance instance = entityBeanComponent.getPool().get();

                //call the ejbCreate method
                final Object primaryKey = invokeEjbCreate(context, ejbCreate, instance, params);
                instance.associate(primaryKey);
                primaryKeyReference.set(primaryKey);

                //now add the instance to the cache, so it is usable
                //note that we do not release it back to the pool
                //the cache will do that when it is expired or removed

                boolean synchronizationRegistered = false;
                boolean exception = false;
                entityBeanComponent.getCache().create(instance);

                //we reference the entity immedietly
                //and release our reference once the create method or the current tx finishes
                entityBeanComponent.getCache().reference(instance);
                try {

                    invokeEjbPostCreate(context, ejbPostCreate, instance, params);

                    //if a transaction is active we register a sync
                    //and if the transaction is rolled back we release the instance back into the pool

                    final TransactionSynchronizationRegistry transactionSynchronizationRegistry = entityBeanComponent.getTransactionSynchronizationRegistry();
                    if (transactionSynchronizationRegistry.getTransactionKey() != null) {
                        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                            @Override
                            public void beforeCompletion() {

                            }

                            @Override
                            public void afterCompletion(final int status) {
                                entityBeanComponent.getCache().release(instance, status == Status.STATUS_COMMITTED);
                                if (status != Status.STATUS_COMMITTED) {
                                    entityBeanComponent.getPool().release(instance);
                                }
                            }
                        });
                        synchronizationRegistered = true;
                    }
                    return context.proceed();
                } catch (Exception e) {
                    entityBeanComponent.getCache().release(instance, false);
                    throw e;
                } finally {
                    if (!synchronizationRegistered && !exception) {
                        entityBeanComponent.getCache().release(instance, true);
                    }
                }
            }


        };

    }

    protected void invokeEjbPostCreate(final InterceptorContext context, final Method ejbPostCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        try {
            ejbPostCreate.invoke(instance.getInstance(), params);
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }

    protected Object invokeEjbCreate(final InterceptorContext context, final Method ejbCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        try {
            return ejbCreate.invoke(instance.getInstance(), params);
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }
}
