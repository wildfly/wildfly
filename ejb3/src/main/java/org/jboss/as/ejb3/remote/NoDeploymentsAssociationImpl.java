/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.InvocationRequest;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.ejb.server.SessionOpenRequest;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A service providing an instance of Association to be used when no deployments ara available.
 *
 * This instance handles the case where there are no deployments on the server containing @Remote EJBs.
 * In such a case, we want to do the following:
 * - for every invocation request, return a NoSuchEJBExceotion
 * - for every session open request, return a NSuchEJBException
 * - when registering a cluster topology listener, do something sensible
 * - when registering a module availability listener, do something sensible
 *
 * @author Richard Achmatowicz
 */
public class NoDeploymentsAssociationImpl implements Association {

    public static Logger logger = Logger.getLogger("org.jboss.as.ejb3.remote.NoDeploymentsAssociationImpl");

    public static NoDeploymentsAssociationImpl INSTANCE = new NoDeploymentsAssociationImpl();

    public NoDeploymentsAssociationImpl() {
    }

    @Override
    public <T> CancelHandle receiveInvocationRequest(InvocationRequest invocationRequest) {
        logger.trace("Calling receiveInvocationRequest");
        // given that th server has no deployments, return NoSuchEJBException
        invocationRequest.writeNoSuchEJB();
        return CancelHandle.NULL;
    }

    @Override
    public CancelHandle receiveSessionOpenRequest(SessionOpenRequest sessionOpenRequest) {
        logger.trace("Calling receiveSessionOpenRequest");
        // given that the server has no deployments, return NoSuchEJBException
        sessionOpenRequest.writeNoSuchEJB();
        return CancelHandle.NULL;
    }

    @Override
    public ListenerHandle registerClusterTopologyListener(ClusterTopologyListener clusterTopologyListener) {
        logger.trace("Calling registerClusterTopologyListener");
        // send an empty topology to the client
        List<ClusterTopologyListener.ClusterInfo> clusterInfoList = new ArrayList<>();
        clusterTopologyListener.clusterTopology(clusterInfoList);
        return () -> {};
    }

    @Override
    public ListenerHandle registerModuleAvailabilityListener(ModuleAvailabilityListener moduleAvailabilityListener) {
        logger.trace("Calling registerModuleAvailabilityListener");
        // send an empty list to the client
        List<EJBModuleIdentifier> moduleList = new ArrayList<EJBModuleIdentifier>();
        moduleAvailabilityListener.moduleAvailable(moduleList);
        return () -> {};
    }
}
