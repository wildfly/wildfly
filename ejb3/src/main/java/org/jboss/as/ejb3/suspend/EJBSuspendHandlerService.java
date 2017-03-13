/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ejb3.suspend;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.CreationListener;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Controls shutdown indicating whether a remote request should be accepted or not.
 *
 * @author Flavia Rainone
 */
public class EJBSuspendHandlerService implements Service<EJBSuspendHandlerService>, ServerActivity, CreationListener,
        Synchronization {

    /**
     * EJBSuspendHandlerService name
     */
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("suspend-handler");

    /**
     * Updates activeInvocationCount field
     */
    private static final AtomicIntegerFieldUpdater<EJBSuspendHandlerService> activeInvocationCountUpdater = AtomicIntegerFieldUpdater
            .newUpdater(EJBSuspendHandlerService.class, "activeInvocationCount");

    /**
     * Updates activeTransactionCount field
     */
    private static final AtomicIntegerFieldUpdater<EJBSuspendHandlerService> activeTransactionCountUpdater = AtomicIntegerFieldUpdater
            .newUpdater(EJBSuspendHandlerService.class, "activeTransactionCount");

    /**
     * Updates listener field
     */
    private static final AtomicReferenceFieldUpdater<EJBSuspendHandlerService, ServerActivityCallback> listenerUpdater = AtomicReferenceFieldUpdater
            .newUpdater(EJBSuspendHandlerService.class, ServerActivityCallback.class, "listener");

    /**
     * Model attribute: if gracefulTxnShutdown attribute is true, handler will bypass module unavailability notification
     * to clients while there still active transactions, giving those transactions a chance to complete before completing
     * suspension.
     */
    private boolean gracefulTxnShutdown;

    /**
     * Injection of suspend controller, for registering server activity
     */
    private final InjectedValue<SuspendController> suspendControllerInjectedValue = new InjectedValue<>();

    /**
     * Injection of local transaction context for keeping track of active transactions
     */
    private final InjectedValue<LocalTransactionContext> localTransactionContextInjectedValue = new InjectedValue<>();

    /**
     * Injection of DeploymentRepository, for suspending and resuming deployments
     */
    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<>();

    /**
     * The number of active requests that are using this entry point
     */
    @SuppressWarnings("unused") private volatile int activeInvocationCount = 0;

    /**
     * The number of active transactions in the server
     */
    @SuppressWarnings("unused") private volatile int activeTransactionCount = 0;

    /**
     * Keeps track of whether the server shutdown controller has requested suspension
     */
    private volatile boolean suspended = false;

    /**
     * Listener to notify when all active transactions are complete
     */
    private volatile ServerActivityCallback listener = null;

    /**
     * Constructor.
     *
     * @param gracefulTxnShutdown value of model attribute
     */
    public EJBSuspendHandlerService(boolean gracefulTxnShutdown) {
        this.gracefulTxnShutdown = gracefulTxnShutdown;
    }

    /**
     * Sets a new value for {@code gracefulTxnShutdown}.
     *
     * @param gracefulTxnShutdown new value of the model attribute
     */
    public void enableGracefulTxnShutdown(boolean gracefulTxnShutdown) {
        this.gracefulTxnShutdown = gracefulTxnShutdown;
    }

    /**
     * Returns suspend controller injected value.
     *
     * @return suspend controller injected value
     */
    public InjectedValue<SuspendController> getSuspendControllerInjectedValue() {
        return suspendControllerInjectedValue;
    }

    /**
     * Returns local transaction context injected value.
     *
     * @return local transaction context injected value
     */
    public InjectedValue<LocalTransactionContext> getLocalTransactionContextInjectedValue() {
        return localTransactionContextInjectedValue;
    }

    /**
     * Returns deployment repository injected value.
     *
     * @return local transaction context injected value
     */
    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjectedValue() {
        return deploymentRepositoryInjectedValue;
    }

    /**
     * Returns service value.
     */
    @Override public EJBSuspendHandlerService getValue() {
        return this;
    }

    /**
     * Starts the service. Registers server activity, sets transaction listener on local transaction context, and creates and
     * installs deployment controller service.
     *
     * @param context start context
     */
    public void start(StartContext context) {
        final SuspendController suspendController = suspendControllerInjectedValue.getValue();
        suspendController.registerActivity(this);
        final LocalTransactionContext localTransactionContext = localTransactionContextInjectedValue.getValue();
        localTransactionContext.registerCreationListener(this);
    }

    /**
     * Stops the service. Unregisters service activity and clears transaction listener.
     * @param context stop context
     */
    public void stop(StopContext context) {
        final SuspendController suspendController = suspendControllerInjectedValue.getValue();
        suspendController.unRegisterActivity(this);
        final LocalTransactionContext localTransactionContext = localTransactionContextInjectedValue.getValue();
        localTransactionContext.removeCreationListener(this);
    }

    /**
     * Pre suspend. Do nothing.
     * @param listener callback listener
     */
    @Override public void preSuspend(ServerActivityCallback listener) {
        listener.done();
    }

    /**
     * Notifies local transaction context that server is suspended, and only completes suspension if
     * there are no active invocations nor transactions.
     *
     * @param listener callback listener
     */
    @Override public void suspended(ServerActivityCallback listener) {
        this.suspended = true;
        listenerUpdater.set(this, listener);
        localTransactionContextInjectedValue.getValue().suspendRequests();

        final int activeInvocationCount = activeInvocationCountUpdater.get(this);
        if (activeInvocationCount == 0) {
            if (gracefulTxnShutdown) {
                if (activeTransactionCountUpdater.get(this) == 0) {
                    this.doneSuspended();
                } else {
                    EjbLogger.ROOT_LOGGER.suspensionWaitingActiveTransactions(activeInvocationCount);
                }
            } else {
                this.doneSuspended();
            }
        }
    }

    /**
     * Notifies local transaction context that server is resumed, and restarts deployment controller.
     */
    @Override public void resume() {
        this.suspended = false;
        localTransactionContextInjectedValue.getValue().resumeRequests();
        ServerActivityCallback listener = listenerUpdater.get(this);
        if (listener != null) {
            listenerUpdater.compareAndSet(this, listener, null);
        }
        deploymentRepositoryInjectedValue.getValue().resume();
    }

    /**
     * Indicates if a invocation should be accepted: which will happen only if server is not suspended, or if the invocation
     * involves a still active transaction.
     *
     * @param context interceptor context
     * @return {@code true} if invocation can be accepted by invoking interceptor
     */
    public boolean acceptInvocation(InterceptorContext context) throws SystemException {
        if (suspended) {
            if (!gracefulTxnShutdown)
                return false;
            // a null listener means that we are done suspending;
            if (listenerUpdater.get(this) == null || activeTransactionCountUpdater.get(this) == 0)
                return false;
            // retrieve attachment only when we are not entirely suspended, meaning we are mid-suspension
            if (!context.hasTransaction()) {
                // all requests with no transaction must be rejected at this point
                // we need also to block requests with new transactions, which is not being done here. Instead,
                // we are relying on a future call to getTransaction in the same thread, before the invocation is executed;
                // this call will throw an exception if the transaction is new, because this suspend handler
                // has invoked clientTransactionContext.suspendRequests
                return false;
            }
        }
        activeInvocationCountUpdater.incrementAndGet(this);
        return true;
    }

    /**
     * Notifies handler that an active invocation is complete.
     */
    public void invocationComplete() {
        int activeInvocations = activeInvocationCountUpdater.decrementAndGet(this);
        if (suspended && activeInvocations == 0 && (!gracefulTxnShutdown || (activeTransactionCountUpdater.get(this) == 0))) {
            doneSuspended();
        }
    }

    /**
     * Notifies handler that a new transaction has been created.
     */
    @Override public void transactionCreated(AbstractTransaction transaction, CreatedBy createdBy) {
        activeTransactionCountUpdater.incrementAndGet(this);
        try {
            transaction.registerSynchronization(this);
        } catch (RollbackException | IllegalStateException e) {
            // it means the transaction is marked for rollback, or is prepared for commit, at this point we cannot register synchronization
            decrementTransactionCount();
        } catch (SystemException e) {
            decrementTransactionCount();
            EjbLogger.ROOT_LOGGER.debug("Unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Notifies handler that an active transaction is about to complete.
     */
    @Override public void beforeCompletion() {

    }

    /**
     * Notifies handler that an active transaction has completed.
     */
    @Override public void afterCompletion(int status) {
        decrementTransactionCount();
    }

    /**
     * Indicates if ejb subsystem is suspended.
     *
     * @return {@code true} if ejb susbsystem suspension is started (regardless of whether it completed or not)
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Completes suspension: stop deployment controller.
     */
    private void doneSuspended() {
        final ServerActivityCallback oldListener = listener;
        if (oldListener != null && listenerUpdater.compareAndSet(this, oldListener, null)) {
            deploymentRepositoryInjectedValue.getValue().suspend();
            oldListener.done();
            EjbLogger.ROOT_LOGGER.suspensionComplete();
        }
    }

    /**
     * Decrements active tranbsaction count and completes suspension if we are suspending and there are no more
     * active transactions left.
     */
    private void decrementTransactionCount() {
        int activeTransactionCount = activeTransactionCountUpdater.decrementAndGet(this);
        if (suspended && activeTransactionCount == 0 && activeInvocationCountUpdater.get(this) == 0) {
            doneSuspended();
        }
    }
}
