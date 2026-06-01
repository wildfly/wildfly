/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.InvocationRequest;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.ejb.server.SessionOpenRequest;
import org.jboss.logging.Logger;

/**
 * This class provides an instance of Association which can be swapped between one of two
 * Association instance implementations:
 * - a NoDeploymentsAssociation instance which is to be used when there are no @Remote beans deployed
 * - a DeploymentsAssociation instance which is to be used when @Remote beans are deployed
 *
 * The need for such a service which can "swap out" different instances of Association is to
 * prevent cache-based service from being started `at boot time, when no deployments are available.
 *
 * Because Association delegates permit registration of cluster topology and module availability handlers,
 * which are then passed to external consumers, methods are provided for refreshing those handler registrations
 * in a way that does not impact the external consumers.
 *
 * @author Richard Achmatowicz
 */
public class DelegatingAssociationImpl implements Association, Consumer<Association> {

    private static final Logger logger = Logger.getLogger("org.jboss.as.ejb3.DelegatingAssociationImpl");

    public static final DelegatingAssociationImpl INSTANCE = new DelegatingAssociationImpl();

    public volatile Association delegate ;
    private volatile Map<ClusterTopologyListener, ListenerHandle> clusterTopologyListeners = new ConcurrentHashMap<>() ;
    private volatile Map<ModuleAvailabilityListener, ListenerHandle> moduleAvailabilityListeners = new ConcurrentHashMap<>() ;

    public DelegatingAssociationImpl() {
        logger.trace("Calling init<>");
        // initialise with an Association delegate which works when no deployments are available
        this.delegate = NoDeploymentsAssociationImpl.INSTANCE;
        logger.trace("Called init<>, delegate = " + this.delegate.getClass().getSimpleName());
    }

    /**
     * Update the delegate and refresh the handlers provided by the previous delegate
     *
     * NOTE: synchronization required to prevent race between accept and listener registration
     *
     * @param association the desired Association delegate
     */
    @Override
    public synchronized void accept(Association association) {
        logger.trace("Accepting Association, class type = " + (association != null ? association.getClass().getName() : "null"));
        // update the handlers established by existing channels with new listeners for cluster topology and module availability
        if (association != null) {
            refreshHandlers(association);
        }
        // make the association current now that we have:
        // (1) sent the last node to leave using the old instance, if required, and
        // (2) updated the delegating listeners with the listeners for the new Association
        if (association != null) {
            delegate = association;
        } else {
            delegate = NoDeploymentsAssociationImpl.INSTANCE;
        }
    }

    public Association getDelegate() {
        return delegate;
    }

    @Override
    public <T> CancelHandle receiveInvocationRequest(InvocationRequest invocationRequest) {
        logger.trace("Calling receiveInvocationRequest");
        return delegate.receiveInvocationRequest(invocationRequest);
    }

    @Override
    public CancelHandle receiveSessionOpenRequest(SessionOpenRequest sessionOpenRequest) {
        logger.trace("Calling receiveSessionOpenRequest");
        return delegate.receiveSessionOpenRequest(sessionOpenRequest);
    }

    /**
     * Register a ListenerHandle with the current Association and wrap it in a DelegatingListenerHandle
     * instance which will:
     * (1) permit delegate handler instances to be updated and
     * (2) allow external objects to receive and use the same ListenerHandle instance.
     *
     * NOTE: synchronization required to prevent race conditions between:
     * (1) calls to accept() from the DeploymentsAssociationImpl and
     * (2) calls to listener registration from the channels
     *
     * @param clusterTopologyListener the cluster topology listener (not {@code null})
     * @return the DelegatingListenerHandle
     */
    @Override
    public synchronized ListenerHandle registerClusterTopologyListener(ClusterTopologyListener clusterTopologyListener) {
        logger.trace("Calling registerClusterTopologyListener");
        // return a DelegatingListenerHandler instance which can be updated
        ListenerHandle handle = delegate.registerClusterTopologyListener(clusterTopologyListener);
        DelegatingListenerHandle delegatingHandle = new DelegatingListenerHandle(handle);
        clusterTopologyListeners.put(clusterTopologyListener, delegatingHandle);
        return delegatingHandle;
    }

    /**
     * Register a ListenerHandle with the current Association and wrap it in a DelegatingListenerHandle
     * instance which will:
     * (1) permit delegate handler instances to be updated and
     * (2) allow external objects to receive and use the same ListenerHandle instance.
     *
     * NOTE: synchronization required to prevent race conditions between:
     * (1) calls to accept() from DeploymentsAssociationImpl and
     * (2) calls to listener registration from the channels
     *
     * @param moduleAvailabilityListener the cluster topology listener (not {@code null})
     * @return the DelegatingListenerHandle
     */
    @Override
    public synchronized ListenerHandle registerModuleAvailabilityListener(ModuleAvailabilityListener moduleAvailabilityListener) {
        logger.trace("Calling registerModuleAvailabilityListener");
        // return a DelegatingListenerHandler instance which can be updated
        ListenerHandle handle = delegate.registerModuleAvailabilityListener(moduleAvailabilityListener);
        DelegatingListenerHandle delegatingHandle = new DelegatingListenerHandle(handle);
        moduleAvailabilityListeners.put(moduleAvailabilityListener, delegatingHandle);
        return delegatingHandle;
    }

    private void refreshHandlers(Association newAssociation) {
        refreshClusterTopologyListeners(newAssociation);
        refreshModuleAvailabilityListeners(newAssociation);
    }

    /**
     * Refresh the cluster topology ListenerHandler instances for the current Association
     */
    private void refreshClusterTopologyListeners(Association newAssociation) {
        logger.trace("Refreshing cluster topology listeners");

        // update the cluster topology ListenerHandles for the new Association
        Map<ClusterTopologyListener,ListenerHandle> refreshedClusterTopologyListeners = new ConcurrentHashMap<>();

        for (Map.Entry<ClusterTopologyListener, ListenerHandle> entry : clusterTopologyListeners.entrySet()) {
            ClusterTopologyListener listener = entry.getKey();
            ListenerHandle handle = entry.getValue();
            DelegatingListenerHandle delegatingHandle = (DelegatingListenerHandle) handle;

            // if the channel closed the handle, do not refresh and remove from the map
            if (delegatingHandle.isHandleClosedByChannel())
                continue;
            ListenerHandle oldHandle = delegatingHandle.getDelegate();
            ListenerHandle newHandle = newAssociation.registerClusterTopologyListener(listener);
            if (!delegatingHandle.compareAndSetDelegate(oldHandle, newHandle)) {
                // channel closed between our read and CAS - discard new handle
                newHandle.close();
                continue;
            }
            // close the old handler, preserving the delegating handler wrapper
            oldHandle.close();
            refreshedClusterTopologyListeners.put(listener, handle);
        }
        // update the full map in one shot
        clusterTopologyListeners = refreshedClusterTopologyListeners;
        logger.trace("Refreshed cluster topology listeners");
    }

    /**
     * Refresh the module availability ListenerHandler instances for the current Association
     */
    private void refreshModuleAvailabilityListeners(Association newAssociation) {
        logger.trace("Refreshing module availability listeners");

        // update the module availability ListenerHandles for the new Association
        Map<ModuleAvailabilityListener,ListenerHandle> refreshedModuleAvailabilityListeners = new ConcurrentHashMap<>();

        for (Map.Entry<ModuleAvailabilityListener, ListenerHandle> entry : moduleAvailabilityListeners.entrySet()) {
            ModuleAvailabilityListener listener = entry.getKey();
            ListenerHandle handle = entry.getValue();
            DelegatingListenerHandle delegatingHandle = (DelegatingListenerHandle) handle;

            // if the channel closed the handle, do not refresh and remove from the map
            if (delegatingHandle.isHandleClosedByChannel())
                continue;
            ListenerHandle oldHandle = delegatingHandle.getDelegate();
            ListenerHandle newHandle = newAssociation.registerModuleAvailabilityListener(listener);
            if (!delegatingHandle.compareAndSetDelegate(oldHandle, newHandle)) {
                // channel closed between our read and CAS - discard new handle
                newHandle.close();
                continue;
            }
            // close the old handler, preserving the delegating handler wrapper
            oldHandle.close();
            refreshedModuleAvailabilityListeners.put(listener, handle);
        }
        // update the full map in one shot
        moduleAvailabilityListeners = refreshedModuleAvailabilityListeners;
        logger.trace("Refreshed module availability listeners");
    }

    public void sendTopologyUpdateIfLastNodeToLeave() {
        if (delegate instanceof DeploymentsAssociationImpl) {
            ((DeploymentsAssociationImpl)delegate).sendTopologyUpdateIfLastNodeToLeave();
        }
    }
}
