/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.EJBException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.InvokeMethodOnTargetInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.tx.OwnableReentrantLock;
import org.jboss.as.naming.ManagedReference;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionComponentInstance extends SessionBeanComponentInstance implements StatefulSessionBeanInstance<SessionID> {
    private static final long serialVersionUID = 3803978357389448971L;

    private final SessionID id;

    private final Interceptor afterBegin;
    private final Interceptor afterCompletion;
    private final Interceptor beforeCompletion;
    private final Interceptor prePassivate;
    private final Interceptor postActivate;
    private final Interceptor ejb2XRemoveInterceptor;

    /**
     * If this is a BMT bean this stores the active transaction
     */
    private volatile Transaction transaction;

    /**
     * The transaction lock for the stateful bean
     */
    private final OwnableReentrantLock lock = new OwnableReentrantLock();

    /**
     * The thread based lock for the stateful bean
     */
    private final Object threadLock = new Object();
    /**
     * State that is used to defer afterCompletion invocation if an invocation is currently in progress.
     *
     * States:
     * 0 = No invocation in progress
     * 1 = Invocation in progress
     * 2 = Invocation in progress, afterCompletion delayed
     *
     */
    private final AtomicInteger invocationSyncState = new AtomicInteger();
    public static final int SYNC_STATE_NO_INVOCATION = 0;
    public static final int SYNC_STATE_INVOCATION_IN_PROGRESS = 1;
    public static final int SYNC_STATE_AFTER_COMPLETE_DELAYED_NO_COMMIT = 2;
    public static final int SYNC_STATE_AFTER_COMPLETE_DELAYED_COMMITTED = 3;

    Object getThreadLock() {
        return threadLock;
    }

    OwnableReentrantLock getLock() {
        return lock;
    }

    AtomicInteger getInvocationSyncState() {
        return invocationSyncState;
    }

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    protected StatefulSessionComponentInstance(final StatefulSessionComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final Map<Object, Object> context) {
        super(component, preDestroyInterceptor, methodInterceptors);

        final SessionID existingSession = (SessionID) context.get(SessionID.class);
        this.id = (existingSession != null) ? existingSession : component.getCache().getIdentifierFactory().get();
        this.afterBegin = component.getAfterBegin();
        this.afterCompletion = component.getAfterCompletion();
        this.beforeCompletion = component.getBeforeCompletion();
        this.prePassivate = component.getPrePassivate();
        this.postActivate = component.getPostActivate();
        this.ejb2XRemoveInterceptor = component.getEjb2XRemoveMethod();
    }

    protected void afterBegin() {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.AFTER_BEGIN);
        try {
            execute(afterBegin, getComponent().getAfterBeginMethod());
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    protected void afterCompletion(boolean committed) {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.AFTER_COMPLETION);
        try {
            execute(afterCompletion, getComponent().getAfterCompletionMethod(), committed);
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    protected void beforeCompletion() {
        CurrentSynchronizationCallback.set(CurrentSynchronizationCallback.CallbackType.BEFORE_COMPLETION);
        try {
            execute(beforeCompletion, getComponent().getBeforeCompletionMethod());
        } finally {
            CurrentSynchronizationCallback.clear();
        }
    }

    @Override
    public void prePassivate() {
        this.execute(prePassivate, null);
    }

    @Override
    public void postActivate() {
        this.execute(postActivate, null);
    }

    private Object execute(final Interceptor interceptor, final Method method, final Object... parameters) {
        if (interceptor == null)
            return null;
        final InterceptorContext interceptorContext = new InterceptorContext();
        //we need the method so this does not count as a lifecycle invocation
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, getComponent());
        interceptorContext.putPrivateData(ComponentInstance.class, this);
        interceptorContext.putPrivateData(InvokeMethodOnTargetInterceptor.PARAMETERS_KEY, parameters);
        interceptorContext.setContextData(new HashMap<String, Object>());
        interceptorContext.setTarget(getInstance());
        final AbstractTransaction transaction = ContextTransactionManager.getInstance().getTransaction();
        interceptorContext.setTransactionSupplier(() -> transaction);
        try {
            return interceptor.processInvocation(interceptorContext);
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    @Override
    public StatefulSessionComponent getComponent() {
        return (StatefulSessionComponent) super.getComponent();
    }

    @Override
    public SessionID getId() {
        return this.id;
    }

    public Interceptor getEjb2XRemoveInterceptor() {
        return ejb2XRemoveInterceptor;
    }

    @Override
    public String toString() {
        return " Instance of " + getComponent().getComponentName() + " {" + id + "}";
    }

    public Object writeReplace() throws ObjectStreamException {
        Set<Object> keys = getComponent().getSerialiableInterceptorContextKeys();
        final Map<Object, Object> serializableInterceptors = new HashMap<Object, Object>();
        for(Object key : keys) {
            serializableInterceptors.put(key, getInstanceData(key));
        }

        return new SerializedStatefulSessionComponent((ManagedReference)getInstanceData(INSTANCE_KEY), id, getComponent().getCreateServiceName().getCanonicalName(), serializableInterceptors);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void removed() {
        TransactionSynchronizationRegistry tsr = this.getComponent().getTransactionSynchronizationRegistry();
        // Trigger preDestroy callback, but only if bean is not associated with a current tx
        // Otherwise, bean destroy is deferred until tx commit
        if ((tsr.getTransactionKey() == null) || (tsr.getResource(this.id) == null)) {
            this.destroy();
        }
    }
}
