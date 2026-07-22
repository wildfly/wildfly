/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.InvocationRequest;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.ejb.server.SessionOpenRequest;
import org.jboss.logging.Logger;

/**
 * An Association which delegates interface methods to a configurable Association instance
 * held as a delegate.
 * Because Association delegates permit registration of cluster topology and module availability handlers,
 * which are then passed to external consumers, methods are provided for refreshing those handler registrations
 * in a way that does not impact the external consumers.
 *
 * @author Richard Achmatowicz
 */
public class DelegatingAssociationImpl implements Association {

    public static Logger logger = Logger.getLogger("org.jboss.as.ejb3.DelegatingAssociationImpl");
    public Association delegate ;
    private Map<ClusterTopologyListener, ListenerHandle> clusterTopologyListeners = new ConcurrentHashMap<>() ;
    private Map<ModuleAvailabilityListener, ListenerHandle> moduleAvailabilityListeners = new ConcurrentHashMap<>() ;


    public DelegatingAssociationImpl() {
        logger.trace("Calling init<>");
        // initialise with an Association delegate which works when no deployments are available
        this.delegate = NoDeploymentsAssociationImpl.INSTANCE;
        logger.trace("Called init<>, delegate = " + this.delegate.getClass().getSimpleName());
    }

    /**
     * Update the delegate and refresh the handlers provided by the previous delegate
     *
     * @param association the desired Association delegate
     */
    void accept(Association association) {
        logger.trace("Accepting Association, class type = " + (association != null ? association.getClass().getName() : "null"));
        // if we are moving from AssociationImpl to NoDeploymentsAssociationImpl, we may be the last node to leave
        if (association instanceof NoDeploymentsAssociationImpl && delegate instanceof DeploymentsAssociationImpl) {
            // we are moving from AssociationImp to NoDeploymentsAssociationImpl as the last deployment on this node has been undeployed
            // we need to check if we are the last node in the cluster and send a removeCluster message to all clients
            ((DeploymentsAssociationImpl)delegate).sendTopologyUpdateIfLastNodeToLeave();
        }
        delegate = association;
        if (association != null) {
            refreshHandlers();
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
     * @param clusterTopologyListener the cluster topology listener (not {@code null})
     * @return the DelegatingListenerHandle
     */
    @Override
    public ListenerHandle registerClusterTopologyListener(ClusterTopologyListener clusterTopologyListener) {
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
     * @param moduleAvailabilityListener the cluster topology listener (not {@code null})
     * @return the DelegatingListenerHandle
     */
    @Override
    public ListenerHandle registerModuleAvailabilityListener(ModuleAvailabilityListener moduleAvailabilityListener) {
        logger.trace("Calling registerModuleAvailabilityListener");
        // return a DelegatingListenerHandler instance which can be updated
        ListenerHandle handle = delegate.registerModuleAvailabilityListener(moduleAvailabilityListener);
        DelegatingListenerHandle delegatingHandle = new DelegatingListenerHandle(handle);
        moduleAvailabilityListeners.put(moduleAvailabilityListener, delegatingHandle);
        return delegatingHandle;
    }

    private void refreshHandlers() {
        refreshClusterTopologyListeners();
        refreshModuleAvailabilityListeners();
    }

    /**
     * Refresh the cluster topology ListenerHandler instances for the current Association
     */
    private void refreshClusterTopologyListeners() {
        logger.trace("Refreshing cluster topology listeners");

        // update the cluster topology ListenerHandles for the new Association
        Map<ClusterTopologyListener,ListenerHandle> refreshedClusterTopologyListeners = new ConcurrentHashMap<>();

        for (Map.Entry<ClusterTopologyListener, ListenerHandle> entry : clusterTopologyListeners.entrySet()) {
            ClusterTopologyListener listener = entry.getKey();
            ListenerHandle handle = entry.getValue();
            // close the old handler
            handle.close();
            // create the new delegating handler
            ((DelegatingListenerHandle) handle).setDelegate(registerClusterTopologyListener(listener));
            // update the map entry for the specific listener
            refreshedClusterTopologyListeners.put(listener, handle);
        }
        // update the full map in one shot
        clusterTopologyListeners = refreshedClusterTopologyListeners;
        logger.trace("Refreshed cluster topology listeners");
    }

    /**
     * Refresh the module availability ListenerHandler instances for the current Association
     */
    private void refreshModuleAvailabilityListeners() {
        logger.trace("Refreshing module availability listeners");

        // update the module availability ListenerHandles for the new Association
        Map<ModuleAvailabilityListener,ListenerHandle> refreshedModuleAvailabilityListeners = new ConcurrentHashMap<>();

        for (Map.Entry<ModuleAvailabilityListener, ListenerHandle> entry : moduleAvailabilityListeners.entrySet()) {
            ModuleAvailabilityListener listener = entry.getKey();
            ListenerHandle handle = entry.getValue();
            // close the old handler
            handle.close();
            // update the delegating handler with the new handler
            ((DelegatingListenerHandle) handle).setDelegate(registerModuleAvailabilityListener(listener));
            // update the map entry for the specific listener
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
