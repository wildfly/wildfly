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

package org.jboss.as.cmp.component;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBException;
import javax.ejb.EntityBean;

import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.bridge.CMRMessage;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.entity.WrappedRemoteException;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponentInstance extends EntityBeanComponentInstance {
    private final Interceptor relationshipInterceptor;

    CmpEntityBeanComponentInstance(final BasicComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, Map<Method, Interceptor> methodInterceptors, final Interceptor relationshipInterceptor) {
        super(component, instanceReference, preDestroyInterceptor, methodInterceptors);
        this.relationshipInterceptor = relationshipInterceptor;
    }

    public CmpEntityBeanComponent getComponent() {
        return (CmpEntityBeanComponent) super.getComponent();
    }

    public CmpEntityBeanContext getEjbContext() {
        return (CmpEntityBeanContext) super.getEjbContext();
    }

    public void setupContext() {
        try {
            final CmpEntityBeanContext context = new CmpEntityBeanContext(this);
            setEjbContext(context);
            getInstance().setEntityContext(context);
            getComponent().getStoreManager().activateEntity(context);
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public EntityBean getInstance() {
        final EntityBean instance = super.getInstance();
        if (instance instanceof CmpProxy) {
            CmpProxy.class.cast(instance).setComponentInstance(this);
        }
        return instance;
    }

    public synchronized void store() {
        try {
            if (!isRemoved()) {

                invokeEjbStore();

                final CmpEntityBeanContext context = getEjbContext();
                final JDBCEntityPersistenceStore store = getComponent().getStoreManager();
                if (context.getPrimaryKey() != null && store.isStoreRequired(context)) {
                    store.storeEntity(context);
                }
            }
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    public void passivate() {
        final JDBCEntityPersistenceStore store = getComponent().getStoreManager();
        try {
            store.passivateEntity(this.getEjbContext());
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    Object invoke(final CMRMessage message, final Object... params) {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(params);
        interceptorContext.putPrivateData(Component.class, getComponent());
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        interceptorContext.putPrivateData(CMRMessage.class, message);

        try {
            return relationshipInterceptor.processInvocation(interceptorContext);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException("Failed to invoke relationship request");
        }
    }
}
