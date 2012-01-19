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

package org.jboss.as.ejb3.component.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanHomeCreateInterceptorFactory;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author John Bailey
 */
public class EntityBeanRemoteViewInstanceFactory implements ViewInstanceFactory {
    private final String applicationName;
    private final String moduleName;
    private final String distinctName;
    private final String beanName;

    public EntityBeanRemoteViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String beanName) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.beanName = beanName;
    }

    public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) throws Exception {
        Object primaryKey = contextData.get(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);
        if (primaryKey == null) {
            primaryKey = invokeCreate(componentView.getComponent(), contextData);
        }
        Object value = EJBClient.createProxy(new EntityEJBLocator(componentView.getViewClass(), applicationName, moduleName, beanName, distinctName, primaryKey));
        return new ValueManagedReference(new ImmediateValue(value));
    }

    private Object invokeCreate(final Component component, final Map<Object, Object> contextData) throws Exception {
        final Method ejbCreate = (Method) contextData.get(EntityBeanHomeCreateInterceptorFactory.EJB_CREATE_METHOD_KEY);
        if (ejbCreate == null) {
            throw new IllegalStateException("Entities can not be created.  No create method available.");
        }
        final Method ejbPostCreate = (Method) contextData.get(EntityBeanHomeCreateInterceptorFactory.EJB_POST_CREATE_METHOD_KEY);
        final Object[] params = (Object[]) contextData.get(EntityBeanHomeCreateInterceptorFactory.PARAMETERS_KEY);

        if (!(component instanceof EntityBeanComponent)) {
            throw new IllegalStateException("Unexpected component: " + component + " Expected " + EntityBeanComponent.class);
        }
        final EntityBeanComponent entityBeanComponent = (EntityBeanComponent) component;
        //grab an unasociated entity bean from the pool
        final EntityBeanComponentInstance instance = entityBeanComponent.acquireUnAssociatedInstance();

        //call the ejbCreate method
        final Object primaryKey;
        primaryKey = invokeEjbCreate(contextData, ejbCreate, instance, params);
        instance.associate(primaryKey);

        //now add the instance to the cache, so it is usable
        //note that we do not release it back to the pool
        //the cache will do that when it is expired or removed
        entityBeanComponent.getCache().create(instance);

        invokeEjbPostCreate(contextData, ejbPostCreate, instance, params);

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
                    if (status != Status.STATUS_COMMITTED) {
                        //if the transaction is rolled back we release the instance back into the pool
                        entityBeanComponent.releaseEntityBeanInstance(instance);
                    }
                }
            });
        }
        return primaryKey;
    }

    protected void invokeEjbPostCreate(final Map<Object, Object> contextData, final Method ejbPostCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {
        try {
            ejbPostCreate.invoke(instance.getInstance(), params);
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }

    protected Object invokeEjbCreate(final Map<Object, Object> contextData, final Method ejbCreate, final EntityBeanComponentInstance instance, final Object[] params) throws Exception {

        //there will always be an invocation
        //as this is only used from home invocations
        final InterceptorContext context = CurrentInvocationContext.get();
        final InvocationType invocationType = context.getPrivateData(InvocationType.class);
        try {
            context.putPrivateData(InvocationType.class, InvocationType.ENTITY_EJB_CREATE);
            return ejbCreate.invoke(instance.getInstance(), params);
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }finally {
            context.putPrivateData(InvocationType.class, invocationType);
        }
    }
}
