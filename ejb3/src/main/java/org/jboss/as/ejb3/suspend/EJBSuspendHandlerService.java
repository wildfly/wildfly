/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.suspend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.protocol.versionone.VersionOneProtocolChannelReceiver;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.ejb.client.AttachmentKeys;
import org.jboss.ejb.client.TransactionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Controls shutdown indicating whether a remote request should be accepted or not.
 *
 * @author Flavia Rainone
 */
public class EJBSuspendHandlerService implements Service<EJBSuspendHandlerService>, ServerActivity {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("remote-shudown-handler");

    private static final AtomicIntegerFieldUpdater<EJBSuspendHandlerService> activeInvocationCountUpdater = AtomicIntegerFieldUpdater
            .newUpdater(EJBSuspendHandlerService.class, "activeInvocationCount");
    private static final AtomicReferenceFieldUpdater<EJBSuspendHandlerService, ServerActivityCallback> listenerUpdater = AtomicReferenceFieldUpdater
            .newUpdater(EJBSuspendHandlerService.class, ServerActivityCallback.class, "listener");

    private boolean gracefulTxnShutdown;
    private EJBRemoteTransactionsRepository transactionsRepository;
    private final InjectedValue<SuspendController> suspendControllerInjectedValue = new InjectedValue<>();
    private final List<VersionOneProtocolChannelReceiver> channelReceivers = new ArrayList<>();

    /**
     * The number of active requests that are using this entry point
     */
    @SuppressWarnings("unused") private volatile int activeInvocationCount = 0;

    // keeps track of whether the server shutdown controller has requested suspension
    private volatile boolean suspended = false;
    // listener to notify when all active transactions are complete
    private volatile ServerActivityCallback listener = null;

    public EJBSuspendHandlerService(boolean gracefulTxnShutdown) {
        this.gracefulTxnShutdown = gracefulTxnShutdown;
    }

    public void enableGracefulTxnShutdown(boolean gracefulTxnShutdown) {
        this.gracefulTxnShutdown = gracefulTxnShutdown;
    }

    public void setEJBRemoteTransactionsRepository(EJBRemoteTransactionsRepository transactionsRepository) {
        this.transactionsRepository = transactionsRepository;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjectedValue() {
        return suspendControllerInjectedValue;
    }

    public synchronized void registerChannelReceiver(final VersionOneProtocolChannelReceiver channelReceiver) {
        this.channelReceivers.add(channelReceiver);
        if (suspended && listener == null) {
            channelReceiver.notifyModulesUnavailable();
        }
    }

    public synchronized void unregisterChannelReceiver(final VersionOneProtocolChannelReceiver channelReceiver) {
        this.channelReceivers.remove(channelReceiver);
    }

    @Override public EJBSuspendHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
        final SuspendController suspendController = suspendControllerInjectedValue.getValue();
        suspendController.registerActivity(this);

    }

    public void stop(StopContext context) {
        final SuspendController suspendController = suspendControllerInjectedValue.getValue();
        suspendController.unRegisterActivity(this);
    }


    @Override public void preSuspend(ServerActivityCallback listener) {
        listener.done();
    }

    @Override public void suspended(ServerActivityCallback listener) {
        this.suspended = true;
        listenerUpdater.set(this, listener);

        if (activeInvocationCountUpdater.get(this) == 0) {
            if (gracefulTxnShutdown) {
                if (transactionsRepository != null && transactionsRepository.getActiveTransactionCount() == 0) {
                    this.doneSuspended();
                }
            } else {
                this.doneSuspended();
            }
        }
    }

    @Override public void resume() {
        this.suspended = false;
        ServerActivityCallback listener = listenerUpdater.get(this);
        if (listener != null) {
            listenerUpdater.compareAndSet(this, listener, null);
        }
        for (VersionOneProtocolChannelReceiver channelReceiver : channelReceivers)
            channelReceiver.notifyModulesAvailable();
    }

    public boolean acceptInvocation(InterceptorContext context) {
        if (suspended) {
            if (!gracefulTxnShutdown)
                return false;
            // a null listener means that we are done suspending; a null transactions repository means it is no point checking for an active transaction bypass
            if (listenerUpdater.get(this) == null || transactionsRepository == null)
                return false;
            // retrieve attachment only when we are not entirely suspended, meaning we are mid-suspension
            TransactionID transactionID = (TransactionID) context.getPrivateData(AttachmentKeys.TRANSACTION_ID_KEY);
            if (!transactionsRepository.isRemoteTransactionActive(transactionID))
                return false;

        }
        activeInvocationCountUpdater.incrementAndGet(this);
        return true;
    }

    public void invocationComplete() {
        int activeInvocations = activeInvocationCountUpdater.decrementAndGet(this);
        if (suspended && activeInvocations == 0 && (!gracefulTxnShutdown || (transactionsRepository == null || transactionsRepository.getActiveTransactionCount() == 0))) {
            doneSuspended();
        }
    }

    public void noActiveTransactions() {
        if (suspended && activeInvocationCountUpdater.get(this) == 0) {
            doneSuspended();
        }
    }

    private void doneSuspended() {
        final ServerActivityCallback oldListener = listener;
        if (listenerUpdater.compareAndSet(this, oldListener, null)) {
            oldListener.done();
            for (VersionOneProtocolChannelReceiver channelReceiver : channelReceivers)
                channelReceiver.notifyModulesUnavailable();
        }
    }

    /**
     * Indicates if ejb subsystem is suspended.
     *
     * @return {@code true} if ejb susbsystem suspension is started (regardless of whether it completed or not)
     */
    public boolean isSuspended() {
        return suspended;
    }
}
