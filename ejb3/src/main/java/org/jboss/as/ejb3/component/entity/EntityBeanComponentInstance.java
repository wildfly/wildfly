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
import java.util.Map;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.ejb.Timer;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.context.EntityContextImpl;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.tx.OwnableReentrantLock;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponentInstance extends EjbComponentInstance {

    /**
     * The primary key of this instance, is it is associated with an object identity
     */
    private volatile Object primaryKey;
    private volatile EntityContextImpl entityContext;
    private volatile boolean removed = false;
    private volatile boolean synchronizeRegistered;
    private volatile boolean reloadRequired = false;


    private volatile boolean invocationInProgress = false;

    private final Object threadLock = new Object();
    private final OwnableReentrantLock lock = new OwnableReentrantLock();


    private final Interceptor ejbStore;
    private final Interceptor ejbActivate;
    private final Interceptor ejbLoad;
    private final Interceptor ejbPassivate;
    private final Interceptor unsetEntityContext;

    protected EntityBeanComponentInstance(final BasicComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, preDestroyInterceptor, methodInterceptors);
        final EntityBeanComponent ejbComponent = (EntityBeanComponent) component;
        this.ejbStore = ejbComponent.getEjbStore();
        this.ejbActivate = ejbComponent.getEjbActivate();
        this.ejbLoad = ejbComponent.getEjbLoad();
        this.ejbPassivate = ejbComponent.getEjbPassivate();
        this.unsetEntityContext = ejbComponent.getUnsetEntityContext();
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

    public void reload() {
        try {
            final EntityBeanComponent component = getComponent();
            final InterceptorContext loadContext = prepareInterceptorContext();
            loadContext.putPrivateData(InvocationType.class, InvocationType.ENTITY_EJB_EJB_LOAD);
            loadContext.setMethod(component.getEjbLoadMethod());
            ejbLoad.processInvocation(loadContext);
            reloadRequired = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isReloadRequired() {
        return reloadRequired;
    }

    public void setReloadRequired(final boolean reloadRequired) {
        this.reloadRequired = reloadRequired;
    }

    public void discard() {
        if (!isDiscarded()) {
            getComponent().getCache().discard(this);
            this.primaryKey = null;
        }
        super.discard();
    }

    @Override
    protected void preDestroy() {
        try {
            invokeUnsetEntityContext();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void invokeUnsetEntityContext() throws Exception {
        final InterceptorContext context = prepareInterceptorContext();
        final EntityBeanComponent component = getComponent();
        context.setMethod(component.getUnsetEntityContextMethod());
        context.putPrivateData(InvocationType.class, InvocationType.UNSET_ENTITY_CONTEXT);
        unsetEntityContext.processInvocation(context);
    }

    /**
     * Associates this entity with a primary key. This method is called when an entity bean is moved from the
     * pool to the entity cache
     *
     * @param primaryKey The primary key to associate the entity with
     */
    public synchronized void associate(Object primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Invoke the ejbActivate and ejbLoad callback methods for the entity bean.
     * This is called when an entity bean is picked up from the entity cache.
     *
     * @param primaryKey The primary key to activate the entity with
     */
    public synchronized void activate(Object primaryKey) {
        this.primaryKey = primaryKey;
        try {
            final InterceptorContext context = prepareInterceptorContext();
            final EntityBeanComponent component = getComponent();
            final Method ejbActivateMethod = component.getEjbActivateMethod();
            context.setMethod(ejbActivateMethod);
            context.putPrivateData(InvocationType.class, InvocationType.ENTITY_EJB_ACTIVATE);
            ejbActivate.processInvocation(context);
            final InterceptorContext loadContext = prepareInterceptorContext();
            loadContext.putPrivateData(InvocationType.class, InvocationType.ENTITY_EJB_EJB_LOAD);
            loadContext.setMethod(component.getEjbLoadMethod());
            ejbLoad.processInvocation(loadContext);
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes the ejbStore method
     */
    public synchronized void store() {
        try {
            if (!removed) {
                invokeEjbStore();
            }
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void invokeEjbStore() throws Exception {
        final InterceptorContext context = prepareInterceptorContext();
        final EntityBeanComponent component = getComponent();
        context.setMethod(component.getEjbStoreMethod());
        ejbStore.processInvocation(context);
    }

    /**
     * Prepares the instance for release by calling the ejbPassivate method.
     * <p/>
     * This method does not actually release this instance into the pool
     */
    public synchronized void passivate() {
        try {
            if (!removed) {
                final InterceptorContext context = prepareInterceptorContext();
                final EntityBeanComponent component = getComponent();
                context.setMethod(component.getEjbPassivateMethod());
                context.putPrivateData(InvocationType.class, InvocationType.ENTITY_EJB_PASSIVATE);
                ejbPassivate.processInvocation(context);
            }
            primaryKey = null;
            removed = false;
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setupContext(final InterceptorContext interceptorContext) {

        final InvocationType invocationType = interceptorContext.getPrivateData(InvocationType.class);
        try {
            interceptorContext.putPrivateData(InvocationType.class, InvocationType.SET_ENTITY_CONTEXT);
            final EntityContextImpl entityContext = new EntityContextImpl(this);
            setEjbContext(entityContext);
            getInstance().setEntityContext(entityContext);
        } catch (RemoteException e) {
            throw new WrappedRemoteException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            interceptorContext.putPrivateData(InvocationType.class, invocationType);
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
            throw EjbLogger.ROOT_LOGGER.cannotCallGetEjbObjectBeforePrimaryKeyAssociation();
        }
        return getComponent().getEJBObject(pk);
    }

    public EJBLocalObject getEjbLocalObject() {
        final Object pk = getPrimaryKey();
        if (pk == null) {
            throw EjbLogger.ROOT_LOGGER.cannotCallGetEjbLocalObjectBeforePrimaryKeyAssociation();
        }
        return getComponent().getEJBLocalObject(pk);
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

    protected void clearPrimaryKey() {
        this.primaryKey = null;
    }

    public boolean isInvocationInProgress() {
        return invocationInProgress;
    }

    public void setInvocationInProgress(boolean invocationInProgress) {
        this.invocationInProgress = invocationInProgress;
    }

    public Object getThreadLock() {
        return threadLock;
    }

    public OwnableReentrantLock getLock() {
        return lock;
    }

    /**
     * Remove all timers for this entity bean. This method is transactional, so if the current TX is rolled back
     * the timers will not be removed
     */
    public void removeAllTimers() {
        //cancel all timers for this entity
        for (final Timer timer : getComponent().getTimerService().getTimers()) {
            if (timer instanceof TimerImpl) {
                TimerImpl timerImpl = (TimerImpl) timer;
                if (timerImpl.getPrimaryKey() != null && timerImpl.getPrimaryKey().equals(getPrimaryKey())) {
                    timer.cancel();
                }
            }
        }
    }
}
