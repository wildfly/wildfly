/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.helpers.domain.impl;

import static org.jboss.as.protocol.ProtocolUtils.unmarshal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentSetPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentSetPlanResult;
import org.jboss.as.controller.client.helpers.domain.DomainUpdateListener;
import org.jboss.as.controller.client.helpers.domain.InvalidDeploymentPlanException;
import org.jboss.as.controller.client.helpers.domain.RollbackCancelledException;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;
import org.jboss.marshalling.Unmarshaller;

/**
 * Utility class that can build a {@link DeploymentPlanResult} from a stream
 * of {@link DomainClientProtocol} responses.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanResultReader {

    private final DeploymentPlan deploymentPlan;
    private final Unmarshaller unmarshaller;

    public DeploymentPlanResultReader(final DeploymentPlan deploymentPlan, final Unmarshaller unmarshaller) {
        assert deploymentPlan != null : "deploymentPlan is null";
        assert unmarshaller != null : "unmarshaller is null";
        this.unmarshaller = unmarshaller;
        this.deploymentPlan = deploymentPlan;
    }

    public DeploymentPlanResult readResult() throws IOException {

        expectHeader(DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_ID);
        UUID planId = unmarshal(unmarshaller, UUID.class);
        if (!deploymentPlan.getId().equals(planId))
            throw new IllegalStateException("Incorrect id " + planId + " for result; expected " + deploymentPlan.getId());
        byte header = unmarshaller.readByte();
        if (header == DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_INVALID) {
            InvalidDeploymentPlanException e = unmarshal(unmarshaller, InvalidDeploymentPlanException.class);
            return new DeploymentPlanResultImpl(deploymentPlan, e);
        }
        else {
            expectHeader(header, DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID);
            Map<UUID, DeploymentSetPlanResult> setResults = new HashMap<UUID, DeploymentSetPlanResult>();
            // Read in the set results
            do {
                header = readDeploymentSetResult(setResults);
            }
            while (header == DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID);

            if (header == DomainClientProtocol.RETURN_DEPLOYMENT_SET_ROLLBACK) {
                do {
                    header = readDeploymentSetRollback(setResults);
                }
                while (header == DomainClientProtocol.RETURN_DEPLOYMENT_SET_ROLLBACK);
            }

            expectHeader(header, DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_COMPLETE);

            // Notify any listeners that actions are complete
            for (DeploymentSetPlan setPlan : deploymentPlan.getDeploymentSetPlans()) {
                for (DeploymentAction action : setPlan.getDeploymentActions()) {
                    for (DomainUpdateListener<?> listener : ((DeploymentActionImpl) action).getListeners()) {
                        listener.handleComplete();
                    }
                }
            }

            return new DeploymentPlanResultImpl(deploymentPlan, setResults);
        }
    }

    private byte readDeploymentSetResult(final Map<UUID, DeploymentSetPlanResult>setResults) throws IOException {
        UUID setId = unmarshal(unmarshaller, UUID.class);

        DeploymentSetPlan setPlan = findDeploymentSet(setId); // will throw ISE if not found

        // A valid deployment set plan will have at least one action
        byte nextHeader = unmarshaller.readByte();
        expectHeader(nextHeader, DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID);

        // Next the server will send all the results of applying set plan updates to DC and HC domain model
        Map<UUID, DeploymentActionResult> actionResults = new HashMap<UUID, DeploymentActionResult>();
        do {
            nextHeader = readDeploymentActionResult(setPlan, actionResults);
        }
        while (nextHeader == DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID);

        // If the set plan generated server updates, those will come next
        if (nextHeader == DomainClientProtocol.RETURN_SERVER_DEPLOYMENT) {
            do {
                nextHeader = readServerDeploymentResult(actionResults);
            }
            while (nextHeader == DomainClientProtocol.RETURN_SERVER_DEPLOYMENT);
        }

        DeploymentSetPlanResult setResult = new DeploymentSetPlanResultImpl(setPlan, actionResults);
        setResults.put(setId, setResult);
        return nextHeader;
    }

    private byte readDeploymentActionResult(final DeploymentSetPlan setPlan, final Map<UUID, DeploymentActionResult> actionResults) throws IOException {

        UUID actionId = unmarshal(unmarshaller, UUID.class);
        DeploymentActionImpl action = findDeploymentAction(actionId, setPlan);
        Set<DomainUpdateListener<?>> listeners = action.getListeners();
        expectHeader(DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT);
        DomainUpdateApplierResponse duar = unmarshal(unmarshaller, DomainUpdateApplierResponse.class);
        DeploymentActionResultImpl actionResult = new DeploymentActionResultImpl(action, duar);
        actionResults.put(actionId, actionResult);

        // Notify any listeners
        for (DomainUpdateListener<?> listener : listeners) {
            if (duar.isCancelled()) {
                listener.handleCancelledByDomain();
            }
            else if (duar.isRolledBack()) {
                listener.handleDomainRollback();
            }
            else if (duar.getDomainFailure() != null) {
                listener.handleDomainFailed(duar.getDomainFailure());
            }
            else if (duar.getHostFailures().size() > 0) {
                listener.handleHostFailed(duar.getHostFailures());
            }
            else {
                listener.handleServersIdentified(duar.getServers());
            }
        }

        return unmarshaller.readByte();
    }

    private byte readServerDeploymentResult(Map<UUID, DeploymentActionResult> actionResults) throws IOException {

        UUID actionId = unmarshal(unmarshaller, UUID.class);
        DeploymentActionResultImpl actionResult = (DeploymentActionResultImpl) actionResults.get(actionId);
        ServerIdentity serverId = readServerIdentity();
        expectHeader(DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_RESULT);
        @SuppressWarnings("unchecked")
        UpdateResultHandlerResponse<Void> urhr = unmarshal(unmarshaller, UpdateResultHandlerResponse.class);

        ServerUpdateResult<Void> sur = new ServerUpdateResultImpl<Void>(actionId, serverId, urhr);
        actionResult.storeServerUpdateResult(serverId, sur);

        // Notifiy and listeners
        DeploymentActionImpl action = (DeploymentActionImpl) actionResult.getDeploymentAction();
        for (DomainUpdateListener<?> listener : action.getListeners()) {
            if (urhr.isCancelled()) {
                listener.handleCancellation(serverId);
            }
            else if (urhr.isRolledBack()) {
                listener.handleRollbackSuccess(serverId);
            }
            else if (urhr.isTimedOut()) {
                listener.handleTimeout(serverId);
            }
            else if (urhr.getFailureResult() != null) {
                listener.handleFailure(urhr.getFailureResult(), serverId);
            }
            else {
                listener.handleSuccess(null, serverId);
            }
        }

        return unmarshaller.readByte();
    }

    private byte readDeploymentSetRollback(Map<UUID, DeploymentSetPlanResult> setResults) throws IOException {
        UUID setId = unmarshal(unmarshaller, UUID.class);

        DeploymentSetPlanResult setResult = setResults.get(setId);
        if (setResult == null) {
            throw new IOException("Unknown deployment set plan " + setId);
        }

        // A valid deployment set plan will have at least one action
        byte nextHeader = unmarshaller.readByte();
        expectHeader(nextHeader, DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID);

        do {
            nextHeader = readDeploymentActionRollback(setResult);
        }
        while (nextHeader != DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID);

        // If the set plan generated server updates, those will come next
        if (nextHeader == DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_ROLLBACK) {
            do {
                nextHeader = readServerDeploymentRollback(setResult);
            }
            while(nextHeader == DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_ROLLBACK);
        }

        return nextHeader;
    }

    private byte readDeploymentActionRollback(DeploymentSetPlanResult setResult) throws IOException {

        UUID actionId = unmarshal(unmarshaller, UUID.class);
        DeploymentActionImpl action = findDeploymentAction(actionId, setResult.getDeploymentSetPlan());
        Set<DomainUpdateListener<?>> listeners = action.getListeners();
        expectHeader(DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT);
        DomainUpdateApplierResponse duar = unmarshal(unmarshaller, DomainUpdateApplierResponse.class);
        DeploymentActionResultImpl actionResult = (DeploymentActionResultImpl) setResult.getDeploymentActionResults().get(actionId);
        if (actionResult != null) {
            actionResult.markRolledBack(duar);

            // Notify any listeners
            for (DomainUpdateListener<?> listener : listeners) {
                if (duar.isCancelled()) {
                    listener.handleDomainRollbackFailed(new RollbackCancelledException("Rollback of deployment action " + actionId + "was cancelled"));
                }
                else if (duar.isRolledBack()) {
                    listener.handleDomainRollbackFailed(new RollbackCancelledException("Rollback of deployment action " + actionId + "was itself rolled back"));
                }
                else if (duar.getDomainFailure() != null) {
                    listener.handleDomainRollbackFailed(duar.getDomainFailure());
                }
                else if (duar.getHostFailures().size() > 0) {
                    listener.handleHostRollbackFailed(duar.getHostFailures());
                }
                else {
                    listener.handleDomainRollback();
                }
            }
        }

        byte nextHeader = unmarshaller.readByte();
        return nextHeader;
    }

    private byte readServerDeploymentRollback(DeploymentSetPlanResult setResult) throws IOException {

        UUID actionId = unmarshal(unmarshaller, UUID.class);
        DeploymentActionResultImpl actionResult = (DeploymentActionResultImpl) setResult.getDeploymentActionResults().get(actionId);
        ServerIdentity serverId = readServerIdentity();
        expectHeader(DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_RESULT);
        @SuppressWarnings("unchecked")
        UpdateResultHandlerResponse<Void> urhr = unmarshal(unmarshaller, UpdateResultHandlerResponse.class);

        if (actionResult != null) {
            // Notifiy and listeners
            DeploymentActionImpl action = (DeploymentActionImpl) actionResult.getDeploymentAction();
            for (DomainUpdateListener<?> listener : action.getListeners()) {
                if (urhr.isCancelled()) {
                    listener.handleRollbackCancellation(serverId);
                }
                else if (urhr.getFailureResult() != null) {
                    listener.handleRollbackFailure(urhr.getFailureResult(), serverId);
                }
                else if (urhr.isRolledBack()) {
                    listener.handleRollbackFailure(new RollbackCancelledException("Rollback of deployment action " + actionId + "was itself rolled back"), serverId);
                }
                else if (urhr.isTimedOut()) {
                    listener.handleRollbackTimeout(serverId);
                }
                else {
                    listener.handleRollbackSuccess(serverId);
                }
            }

            ServerGroupDeploymentActionResult sgdar = actionResult.getResultsByServerGroup().get(serverId.getServerGroupName());
            ServerUpdateResultImpl<Void> sur = (ServerUpdateResultImpl<Void>) sgdar.getResultByServer().get(serverId.getServerName());
            sur.markRolledBack(urhr);
        }


        return unmarshaller.readByte();
    }

    private ServerIdentity readServerIdentity() throws IOException {
        expectHeader(DomainClientProtocol.RETURN_HOST_NAME);
        // Note: all StreamedResponse object values are marshalled as object, not UTF
        String hostName = unmarshal(unmarshaller, String.class);
        expectHeader(DomainClientProtocol.RETURN_SERVER_GROUP_NAME);
        String groupName = unmarshal(unmarshaller, String.class);
        expectHeader(DomainClientProtocol.RETURN_SERVER_NAME);
        String serverName = unmarshal(unmarshaller, String.class);
        return new ServerIdentity(hostName, groupName, serverName);
    }

    private DeploymentSetPlan findDeploymentSet(UUID setId) {
        for (DeploymentSetPlan setPlan : deploymentPlan.getDeploymentSetPlans()) {
            if (setId.equals(setPlan.getId()))
                return setPlan;
        }
        throw new IllegalStateException("Deployment plan result included unknown deployment set id " + setId);
    }

    private DeploymentActionImpl findDeploymentAction(final UUID actionId, final DeploymentSetPlan setPlan) {
        for (DeploymentAction action : setPlan.getDeploymentActions()) {
            if (actionId.equals(action.getId()))
                return (DeploymentActionImpl) action;
        }
        throw new IllegalStateException("Deployment set plan result included unknown action id " + actionId);
    }

    void expectHeader(int expected) throws IOException {
        expectHeader(unmarshaller.readByte(), expected);
    }

    private static void expectHeader(final byte actual, int expected) throws IOException {
        if (actual != (byte) expected) {
            throw new IOException("Invalid byte token.  Expecting '" + expected + "' received '" + actual + "'");
        }
    }
}
