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

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.context.EntityContextImpl;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponentInstance extends EjbComponentInstance {

    /**
     * The primary key of this instance, is it is associated with an object identity
     */
    private volatile Object primaryKey;
    private volatile boolean isDiscarded;
    private volatile EntityContextImpl entityContext;
    private volatile boolean removed = false;
    private volatile boolean synchronizeRegistered;

    protected EntityBeanComponentInstance(final BasicComponent component, final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, instanceReference, preDestroyInterceptor, methodInterceptors, Collections.<Method, Interceptor>emptyMap());
    }

    @Override
    public EntityBeanComponent getComponent() {
        return (EntityBeanComponent) super.getComponent();
    }

    @Override
    public EntityBean getInstance() {
        return (EntityBean) super.getInstance();
    }

    public Object getPrimaryKey() {
        return primaryKey;
    }


    public void discard() {
        if (!isDiscarded) {
            isDiscarded = true;
            getComponent().getCache().discard(this);
            this.primaryKey = null;
        }
    }

    @Override
    public void destroy() {
        try {
            getInstance().unsetEntityContext();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        super.destroy();
    }

    /**
     * Associates this entity with a primary key. This method is called when an entity bean is moved from the
     * pool to the entity cache
     *
     * @param primaryKey The primary key to associate the entity with
     */
    public synchronized void associate(Object primaryKey) {
        this.primaryKey = primaryKey;
        EntityBean instance = getInstance();
        try {
            instance.ejbActivate();
            instance.ejbLoad();
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        }
    }

    /**
     * Invokes the ejbStore method
     */
    public synchronized void store() {
        EntityBean instance = getInstance();
        try {
            if (!removed) {
                instance.ejbStore();
            }
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        }
    }

    /**
     * Prepares the instance for release by calling the ejbPassivate method.
     * <p/>
     * This method does not actually release this instance into the pool
     */
    public synchronized void passivate() {
        EntityBean instance = getInstance();
        try {
            if (!removed) {
                instance.ejbPassivate();
            }
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        }
    }

    public void setupContext() {
        try {
            final EntityContextImpl entityContext = new EntityContextImpl(this);
            setEjbContext(entityContext);
            getInstance().setEntityContext(entityContext);
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        }
    }

    public EntityContextImpl getEjbContext() {
        return entityContext;
    }

    protected void setEjbContext(EntityContextImpl entityContext) {
        this.entityContext = entityContext;
    }

    public EJBObject getEjbObject() {
        final Object pk = getPrimaryKey();
        if (pk == null) {
            throw new IllegalStateException("Cannot call getEjbObject before the object is associated with a primary key");
        }
        return getComponent().getEJBObject(pk);
    }

    public EJBLocalObject getEjbLocalObject() {
        final Object pk = getPrimaryKey();
        if (pk == null) {
            throw new IllegalStateException("Cannot call getEjbLocalObject before the object is associated with a primary key");
        }
        return getComponent().getEjbLocalObject(pk);
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(final boolean removed) {
        this.removed = removed;
    }

    public synchronized void setSynchronizationRegistered(final boolean synchronizeRegistered) {
        this.synchronizeRegistered = synchronizeRegistered;
    }

    public synchronized boolean isSynchronizeRegistered() {
        return synchronizeRegistered;
    }
}
