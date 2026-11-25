/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.CancelHandle;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.InvocationRequest;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.ejb.server.SessionOpenRequest;

import java.util.List;

/**
 * A service providing an instance of Association to be used when no deployments are available.
 *
 * This instance handles the case where there are no deployments on the server containing @Remote EJBs.
 * In such a case, we want to do the following:
 * - for every invocation request, return a NoSuchEJBException
 * - for every session open request, return a NoSuchEJBException
 * - when registering a cluster topology listener, return a representation of an empty topology
 * - when registering a module availability listener, return a representation of an empty topology
 *
 * @author Richard Achmatowicz
 */
public class NoDeploymentsAssociationImpl implements Association {

    public static final NoDeploymentsAssociationImpl INSTANCE = new NoDeploymentsAssociationImpl();

    public NoDeploymentsAssociationImpl() {
    }

    @Override
    public <T> CancelHandle receiveInvocationRequest(InvocationRequest invocationRequest) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("NoDeploymentsAssociationImpl: Calling receiveInvocationRequest");

        // given that th server has no deployments, return NoSuchEJBException
        invocationRequest.writeNoSuchEJB();
        return CancelHandle.NULL;
    }

    @Override
    public CancelHandle receiveSessionOpenRequest(SessionOpenRequest sessionOpenRequest) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("NoDeploymentsAssociationImpl: Calling receiveSessionOpenRequest");

        // given that the server has no deployments, return NoSuchEJBException
        sessionOpenRequest.writeNoSuchEJB();
        return CancelHandle.NULL;
    }

    @Override
    public ListenerHandle registerClusterTopologyListener(ClusterTopologyListener clusterTopologyListener) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("NoDeploymentsAssociationImpl: Calling registerClusterTopologyListener");

        // send an empty topology to the client
        clusterTopologyListener.clusterTopology(List.of());
        return () -> {};
    }

    @Override
    public ListenerHandle registerModuleAvailabilityListener(ModuleAvailabilityListener moduleAvailabilityListener) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("NoDeploymentsAssociationImpl: Calling registerModuleAvailabilityListener");

        // send an empty list to the client
        moduleAvailabilityListener.moduleAvailable(List.of());
        return () -> {};
    }
}
