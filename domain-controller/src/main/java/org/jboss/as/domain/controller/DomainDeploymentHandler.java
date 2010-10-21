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

package org.jboss.as.domain.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;
import org.jboss.as.deployment.client.impl.DeploymentActionImpl;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.deployment.DeploymentAction;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.api.deployment.DeploymentSetPlan;
import org.jboss.as.domain.client.api.deployment.IncompleteDeploymentReplaceException;
import org.jboss.as.domain.client.api.deployment.InvalidDeploymentPlanException;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlan;
import org.jboss.as.domain.client.impl.DomainClientProtocol;
import org.jboss.as.domain.client.impl.DomainUpdateApplierResponse;
import org.jboss.as.domain.client.impl.UpdateResultHandlerResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DomainDeploymentAdd;
import org.jboss.as.model.DomainDeploymentFullReplaceUpdate;
import org.jboss.as.model.DomainDeploymentRedeployUpdate;
import org.jboss.as.model.DomainDeploymentRemove;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.DomainServerGroupUpdate;
import org.jboss.as.model.ServerGroupDeploymentAdd;
import org.jboss.as.model.ServerGroupDeploymentRemove;
import org.jboss.as.model.ServerGroupDeploymentReplaceUpdate;
import org.jboss.as.model.ServerGroupDeploymentStartStopUpdate;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.logging.Logger;

/**
 * Handles the DomainController's execution of a {@link DeploymentPlan}.
 *
 * @author Brian Stansberry
 */
public class DomainDeploymentHandler {

    static Logger logger = Logger.getLogger("org.jboss.as.domain.deployment");

    private final DomainController domainController;
    private final ExecutorService executorService;

    public DomainDeploymentHandler(final DomainController domainController, final ExecutorService executorService) {
        this.domainController = domainController;
        this.executorService = executorService;
    }

    public void executeDeploymentPlan(final DeploymentPlan plan, final BlockingQueue<List<StreamedResponse>> responseQueue) {

        // Run the plan in a separate thread so caller can process responses
        Runnable r = new Runnable() {
            @Override
            public void run() {
                runDeploymentPlan(plan, responseQueue);
            }
        };

        getDeploymentExecutor().submit(r);
    }

    /** The actual deployment plan execution logic */
    private void runDeploymentPlan(final DeploymentPlan plan, final BlockingQueue<List<StreamedResponse>> responseQueue) {

        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_ID, plan.getId()));

        List<DeploymentSetPlan> setPlans = plan.getDeploymentSetPlans();
        List<DeploymentSetUpdates> updateSets = new ArrayList<DeploymentSetUpdates>(setPlans.size());
        for (DeploymentSetPlan setPlan : setPlans) {
            try {
                updateSets.add(createDeploymentSetUpdates(setPlan, getDomainModel()));
            } catch (InvalidDeploymentPlanException e) {
                pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_INVALID, e, true));
                return;
            }
        }

        List<DeploymentSetUpdates> rollbackSets = new ArrayList<DeploymentSetUpdates>(setPlans.size());
        boolean ok = true;
        for (DeploymentSetUpdates updateSet : updateSets) {
            if (ok) {
                ok = executeDeploymentSet(updateSet, responseQueue);
                if (ok) {
                    rollbackSets.add(0, updateSet); // roll back in reverse order
                }
            } else {
                // A previous set failed; just inform client this set is cancelled
                cancelDeploymentSet(updateSet, responseQueue);
            }
        }

        if (!ok && plan.isGlobalRollback()) {
            // Rollback the sets that succeeded before the one that failed
            // The one that failed will have rolled itself back.
            for (DeploymentSetUpdates updateSet : rollbackSets) {
                rollbackDeploymentSet(updateSet, responseQueue);
            }
        }

        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_COMPLETE, null, true));
    }

    private DomainModel getDomainModel() {
        return domainController.getDomainModel();
    }

    private boolean executeDeploymentSet(final DeploymentSetUpdates updateSet, final BlockingQueue<List<StreamedResponse>> responseQueue) {

        // Execute domain model update on domain controller and server managers
        List<DomainUpdateApplierResponse> rsps = domainController.applyUpdatesToModel(updateSet.getDomainUpdates());

        // Inform client of results
        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID, updateSet.setPlan.getId()));
        List<StreamedResponse> rspList = new ArrayList<StreamedResponse>();
        DeploymentAction lastResponseAction = null;
        for (int i = 0; i < rsps.size(); i++) {
            DomainUpdateApplierResponse duar = rsps.get(i);
            // There can be multiple domain updates for a given action, but we
            // only send one response. Use this update result for the response if
            // 1) it failed or 2) it's the last update associated with the action
            if (duar.getDomainFailure() != null || duar.getHostFailures().size() > 0 || updateSet.isLastDomainUpdateForAction(i)) {
                DeploymentAction action = updateSet.getDeploymentActionForDomainUpdate(i);
                if (action != lastResponseAction) {
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID, action.getId()));
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT, duar));
                    lastResponseAction = action;
                }
            }
        }

        DomainUpdateApplierResponse last = rsps.get(rsps.size() - 1);
        if (last.getDomainFailure() != null || last.getHostFailures().size() > 0) {
            // DomainModel update failed; don't apply to servers. The DomainController will
            // have already rolled back the domain model update
            return false;
        }

        // Apply to servers
        Runnable r = getServerUpdateTask(updateSet, rsps, responseQueue);
        r.run();

        // FIXME If application to servers was not ok, roll back
        throw new UnsupportedOperationException("handle rollback");
    }

    private ExecutorService getDeploymentExecutor() {
        return executorService;
    }

    private void cancelDeploymentSet(final DeploymentSetUpdates updateSet, final BlockingQueue<List<StreamedResponse>> responseQueue) {

        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID, updateSet.setPlan.getId()));

        List<StreamedResponse> rspList = new ArrayList<StreamedResponse>();
        DomainUpdateApplierResponse duar = new DomainUpdateApplierResponse(true);
        for (ActionUpdates au : updateSet.actionUpdates) {
            rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID, au.action.getId()));
            rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT, duar));
        }
    }

    private void rollbackDeploymentSet(final DeploymentSetUpdates updateSet, final BlockingQueue<List<StreamedResponse>> responseQueue) {
        // FIXME implement rollbackDeploymentSet
        throw new UnsupportedOperationException("implement me");
    }

    private Runnable getServerUpdateTask(final DeploymentSetUpdates updateSet, final List<DomainUpdateApplierResponse> rsps, final BlockingQueue<List<StreamedResponse>> responseQueue) {

        // Organize all the impacted servers by ServerGroup, sorted within a group by ServerManager
        Map<String, SortedSet<ServerIdentity>> serversByGroup = new HashMap<String, SortedSet<ServerIdentity>>();
        for (DomainUpdateApplierResponse duar : rsps) {
            for (ServerIdentity serverId : duar.getServers()) {
                SortedSet<ServerIdentity> set = serversByGroup.get(serverId.getServerGroupName());
                if (set == null) {
                    set = new TreeSet<ServerIdentity>(ServerIdentityComparator.INSTANCE);
                    serversByGroup.put(serverId.getServerGroupName(), set);
                }
                set.add(serverId);
            }
        }

        // What kind of tasks we create depends on whether shutdown is configured
        boolean shutdown = updateSet.setPlan.isShutdown();
        long gracefulTimeout = updateSet.setPlan.getGracefulShutdownTimeout();

        List<Runnable> masterList = new ArrayList<Runnable>();
        ConcurrentGroupServerUpdatePolicy predecessor = null;
        for (Set<ServerGroupDeploymentPlan> groupPlans : updateSet.setPlan.getServerGroupDeploymentPlans()) {

            List<Runnable> concurrentList = new ArrayList<Runnable>(groupPlans.size());
            ConcurrentGroupServerUpdatePolicy parent = new ConcurrentGroupServerUpdatePolicy(predecessor, groupPlans);
            predecessor = parent;

            for (ServerGroupDeploymentPlan groupPlan : groupPlans) {
                String serverGroupName = groupPlan.getServerGroupName();
                SortedSet<ServerIdentity> servers = serversByGroup.get(serverGroupName);
                ServerUpdatePolicy policy = new ServerUpdatePolicy(parent, serverGroupName, servers, groupPlan);

                List<Runnable> groupTasks = new ArrayList<Runnable>(servers.size());
                if (shutdown) {
                    for (ServerIdentity server : servers) {
                        groupTasks.add (new ServerRestartTask(server, updateSet, policy, responseQueue, gracefulTimeout));
                    }
                }
                else {
                    for (ServerIdentity server : servers) {
                        groupTasks.add (new RunningServerUpdateTask(server, updateSet, policy, responseQueue, groupPlan.isRollback()));
                    }
                }

                if (groupPlan.isRollingToServers()) {
                    concurrentList.add(new RollingUpdateTask(groupTasks));
                }
                else {
                    concurrentList.add(new ConcurrentUpdateTask(groupTasks, getDeploymentExecutor()));
                }
            }
            masterList.add(new ConcurrentUpdateTask(concurrentList, getDeploymentExecutor()));
        }

        return new RollingUpdateTask(masterList);
    }

    private static void pushSingleResponse(BlockingQueue<List<StreamedResponse>> queue, StreamedResponse response) {
        queue.add(Collections.singletonList(response));
    }

    /** Performs the translation from DeploymentAction to domain and server model updates */
    private static DeploymentSetUpdates createDeploymentSetUpdates(DeploymentSetPlan plan, DomainModel model) throws InvalidDeploymentPlanException {
        List<DeploymentAction> actions = plan.getDeploymentActions();

        if (actions.size() == 0) {
            throw new InvalidDeploymentPlanException(String.format("%s %s contains no deployment actions", DeploymentSetPlan.class.getSimpleName(), plan.getId()));
        }

        List<ActionUpdates> actionUpdates = new ArrayList<ActionUpdates>();

        for (DeploymentAction action : actions) {
            ActionUpdates au = new ActionUpdates(action);
            actionUpdates.add(au);

            DeploymentActionImpl dai = (DeploymentActionImpl) action;
            switch (action.getType()) {
                case ADD: {
                    String deploymentName = dai.getDeploymentUnitUniqueName();
                    String runtimeName = dai.getNewContentFileName();
                    byte[] hash = dai.getNewContentHash();
                    if (runtimeName == null) {
                        // This is a request to add already existing content to this set's server groups
                        DeploymentUnitElement de = model.getDeployment(deploymentName);
                        if (de == null) {
                            throw new InvalidDeploymentPlanException("Unknown deployment unit " + deploymentName);
                        }
                        runtimeName = de.getRuntimeName();
                        hash = de.getSha1Hash();
                    }
                    else if (model.getDeployment(deploymentName) == null) {
                        // Deployment is new to the domain; add it
                        au.domainUpdates.add(new DomainDeploymentAdd(deploymentName, runtimeName, hash, false));
                    }
                    // Now add to serve groups
                    ServerGroupDeploymentAdd sgda = new ServerGroupDeploymentAdd(deploymentName, runtimeName, hash, false);
                    addServerGroupUpdates(plan, au, sgda);
                    break;
                }
                case DEPLOY: {
                    ServerGroupDeploymentStartStopUpdate sgdssu = new ServerGroupDeploymentStartStopUpdate(dai.getDeploymentUnitUniqueName(), true);
                    addServerGroupUpdates(plan, au, sgdssu);
                    break;
                }
                case FULL_REPLACE: {
                    // Validate all relevant server groups are touched
                    String deploymentName = dai.getDeploymentUnitUniqueName();
                    Set<String> names = new LinkedHashSet<String>(model.getServerGroupNames());
                    for (Set<ServerGroupDeploymentPlan> ssgp : plan.getServerGroupDeploymentPlans()) {
                        for (ServerGroupDeploymentPlan sgdp : ssgp) {
                            names.remove(sgdp.getServerGroupName());
                        }
                    }
                    for (Iterator<String> it = names.iterator(); it.hasNext();) {
                        String name = it.next();
                        ServerGroupElement sge = model.getServerGroup(name);
                        if (sge.getDeployment(dai.getDeploymentUnitUniqueName()) == null) {
                            it.remove();
                        }
                    }
                    if (names.size() > 0) {
                        throw new IncompleteDeploymentReplaceException(deploymentName, names.toArray(new String[names.size()]));
                    }
                    DeploymentUnitElement deployment = model.getDeployment(dai.getDeploymentUnitUniqueName());
                    boolean start = deployment != null && deployment.isStart();
                    DomainDeploymentFullReplaceUpdate update = new DomainDeploymentFullReplaceUpdate(deploymentName, dai.getNewContentFileName(), dai.getNewContentHash(), start);
                    au.domainUpdates.add(update);
                    au.serverUpdates.add(update.getServerModelUpdate());
                    break;
                }
                case REDEPLOY: {
                    DomainDeploymentRedeployUpdate update = new DomainDeploymentRedeployUpdate(dai.getDeploymentUnitUniqueName());
                    au.domainUpdates.add(update);
                    au.serverUpdates.add(update.getServerModelUpdate());
                    break;
                }
                case REMOVE: {
                    ServerGroupDeploymentRemove sgdr = new ServerGroupDeploymentRemove(dai.getDeploymentUnitUniqueName());
                    addServerGroupUpdates(plan, au, sgdr);
                    // If no server group is using this, remove from domain
                    Set<String> names = model.getServerGroupNames();
                    for (Set<ServerGroupDeploymentPlan> ssgp : plan.getServerGroupDeploymentPlans()) {
                        for (ServerGroupDeploymentPlan sgdp : ssgp) {
                            names.remove(sgdp.getServerGroupName());
                        }
                    }
                    boolean left = false;
                    for (String name : names) {
                        ServerGroupElement sge = model.getServerGroup(name);
                        if (sge.getDeployment(dai.getDeploymentUnitUniqueName()) != null) {
                            left = true;
                            break;
                        }
                    }
                    if (!left) {
                        au.domainUpdates.add(new DomainDeploymentRemove(dai.getDeploymentUnitUniqueName()));
                    }
                    break;
                }
                case REPLACE: {
                    ServerGroupDeploymentReplaceUpdate sgdru = new ServerGroupDeploymentReplaceUpdate(dai.getDeploymentUnitUniqueName(), dai.getNewContentFileName(), dai.getNewContentHash(), dai.getReplacedDeploymentUnitUniqueName());
                    addServerGroupUpdates(plan, au, sgdru);
                    break;
                }
                case UNDEPLOY: {
                    ServerGroupDeploymentStartStopUpdate sgdssu = new ServerGroupDeploymentStartStopUpdate(dai.getDeploymentUnitUniqueName(), false);
                    addServerGroupUpdates(plan, au, sgdssu);
                    break;
                }
                default:
                    throw new IllegalStateException(String.format("Unknown %s %s", DeploymentAction.class.getSimpleName(), action.getType()));
            }
        }

        return new DeploymentSetUpdates(actionUpdates, plan);
    }

    private static void addServerGroupUpdates(final DeploymentSetPlan plan, final ActionUpdates au, final AbstractModelUpdate<ServerGroupElement, ServerDeploymentActionResult> serverGroupUpdate) {
        AbstractServerModelUpdate<?> smu = null;
        for (Set<ServerGroupDeploymentPlan> ssgp : plan.getServerGroupDeploymentPlans()) {
            for (ServerGroupDeploymentPlan sgdp : ssgp) {
                AbstractDomainModelUpdate<?> dmu = new DomainServerGroupUpdate<ServerDeploymentActionResult>(sgdp.getServerGroupName(), serverGroupUpdate);
                au.domainUpdates.add(dmu);
                if (smu == null) {
                    smu = dmu.getServerModelUpdate();
                    au.serverUpdates.add(smu);
                }
            }
        }
    }

    /** Holder for information about a deployment set */
    private static class DeploymentSetUpdates {
        private final List<ActionUpdates> actionUpdates;
        private final DeploymentSetPlan setPlan;
        private final Map<ServerIdentity, List<UpdateResultHandlerResponse<?>>> serverResults = new ConcurrentHashMap<ServerIdentity, List<UpdateResultHandlerResponse<?>>>();

        private DeploymentSetUpdates(final List<ActionUpdates> actionUpdates, final DeploymentSetPlan setPlan) {
            this.actionUpdates = actionUpdates;
            this.setPlan = setPlan;
        }

        private List<AbstractDomainModelUpdate<?>> getDomainUpdates() {
            List<AbstractDomainModelUpdate<?>>result = new ArrayList<AbstractDomainModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                result.addAll(au.domainUpdates);
            }
            return result;
        }

        private List<AbstractServerModelUpdate<?>> getServerUpdates() {
            List<AbstractServerModelUpdate<?>>result = new ArrayList<AbstractServerModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                result.addAll(au.serverUpdates);
            }
            return result;
        }

        private boolean isLastDomainUpdateForAction(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                count += au.domainUpdates.size();
                if (index < count)
                    return (index == count - 1);
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last domain update (" + (getDomainUpdates().size() - 1) + ")");
        }

        private DeploymentAction getDeploymentActionForDomainUpdate(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                count += au.domainUpdates.size();
                if (index < count) {
                    return au.action;
                }
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last domain update (" + (getDomainUpdates().size() - 1) + ")");
        }

        private boolean isLastServerUpdateForAction(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                count += au.serverUpdates.size();
                if (index < count)
                    return (index == count - 1);
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last server update (" + (getServerUpdates().size() - 1) + ")");
        }

        private DeploymentAction getDeploymentActionForServerUpdate(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                count += au.serverUpdates.size();
                if (index < count) {
                    return au.action;
                }
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last domain update (" + (getServerUpdates().size() - 1) + ")");
        }
    }

    /** Simple data class to associate an action with the relevant update objects */
    private static class ActionUpdates {
        private final DeploymentAction action;
        private final List<AbstractDomainModelUpdate<?>> domainUpdates = new ArrayList<AbstractDomainModelUpdate<?>>();
        private final List<AbstractServerModelUpdate<?>> serverUpdates = new ArrayList<AbstractServerModelUpdate<?>>();

        private ActionUpdates(DeploymentAction action) {
            this.action = action;
        }
    }

    private abstract class AbstractServerUpdateTask implements Runnable {
        protected final ServerUpdatePolicy updatePolicy;
        protected final ServerIdentity serverId;
        protected final DeploymentSetUpdates updates;
        protected final BlockingQueue<List<StreamedResponse>> responseQueue;

        AbstractServerUpdateTask(final ServerIdentity serverId, final DeploymentSetUpdates updates,
                final ServerUpdatePolicy updatePolicy, final BlockingQueue<List<StreamedResponse>> responseQueue) {
            this.serverId = serverId;
            this.updatePolicy = updatePolicy;
            this.responseQueue = responseQueue;
            this.updates = updates;
        }

        @Override
        public void run() {
            if (updatePolicy.canUpdateServer(serverId)) {
                processUpdates();
            }
            else {
                sendCancelledResponses();
            }
        }

        protected abstract void processUpdates();

        private void sendCancelledResponses() {
            UpdateResultHandlerResponse<Void> urhr = UpdateResultHandlerResponse.createCancellationResponse();
            for (ActionUpdates au : updates.actionUpdates) {
                sendServerUpdateResult(au.action.getId(), urhr);
                if (Thread.interrupted()) {
                    break;
                }
            }
        }

        protected void sendServerUpdateResult(UUID actionId, UpdateResultHandlerResponse<?> urhr) {
            List<StreamedResponse> responses = new ArrayList<StreamedResponse>();
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_DEPLOYMENT, actionId));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_HOST_NAME, serverId.getHostName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_GROUP_NAME, serverId.getServerGroupName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_NAME, serverId.getServerName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_RESULT, urhr));
            try {
                responseQueue.put(responses);
            } catch (InterruptedException e) {
                logger.errorf("%s interrupted sending cancellation responses for %s %s", toString(), DeploymentSetPlan.class.getSimpleName(), updates.setPlan.getId());
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{server=");
            sb.append(serverId.getServerName());
            sb.append("}");
            return sb.toString();
        }
    }

    /** Applies updates a running server */
    private class RunningServerUpdateTask extends AbstractServerUpdateTask {

        private final boolean allowOverallRollback;

        private RunningServerUpdateTask(final ServerIdentity serverId, final DeploymentSetUpdates updates,
                final ServerUpdatePolicy updatePolicy, final BlockingQueue<List<StreamedResponse>> responseQueue,
                final boolean allowOverallRollback) {
            super(serverId, updates, updatePolicy, responseQueue);
            this.allowOverallRollback = allowOverallRollback;
        }

        @Override
        protected void processUpdates() {
            List<UpdateResultHandlerResponse<?>> rsps =
                domainController.applyUpdatesToServer(serverId, updates.getServerUpdates(), allowOverallRollback);
            updates.serverResults.put(serverId, rsps);
            updatePolicy.recordServerResult(serverId, rsps);

            // Push responses to client
            DeploymentAction lastResponseAction = null;
            for (int i = 0; i < rsps.size(); i++) {
                UpdateResultHandlerResponse<?> urhr = rsps.get(i);
                // There can be multiple server updates for a given action, but we
                // only send one response. Use this update result for the response if
                // 1) it failed or 2) it's the last update associated with the action
                if (urhr.getFailureResult() != null || updates.isLastServerUpdateForAction(i)) {
                    DeploymentAction action = updates.getDeploymentActionForServerUpdate(i);
                    if (action != lastResponseAction) {
                        sendServerUpdateResult(action.getId(), urhr);
                        lastResponseAction = action;
                    }
                }
            }
        }
    }

    /** Restarts a server */
    private class ServerRestartTask extends AbstractServerUpdateTask {

        private final long gracefulTimeout;

        private ServerRestartTask(final ServerIdentity serverId, final DeploymentSetUpdates updates,
                final ServerUpdatePolicy updatePolicy, final BlockingQueue<List<StreamedResponse>> responseQueue,
                final long gracefulTimeout) {
            super(serverId, updates, updatePolicy, responseQueue);
            this.gracefulTimeout = gracefulTimeout;
        }

        @Override
        protected void processUpdates() {

            UpdateResultHandlerResponse<?> urhr = domainController.restartServer(serverId, gracefulTimeout);
            List<UpdateResultHandlerResponse<?>> rsps = Collections.<UpdateResultHandlerResponse<?>>singletonList(urhr);
            updates.serverResults.put(serverId, rsps);
            updatePolicy.recordServerResult(serverId, rsps);

            for (ActionUpdates au : updates.actionUpdates) {
                sendServerUpdateResult(au.action.getId(), urhr);
                if (Thread.interrupted()) {
                    break;
                }
            }
        }
    }

    /** Used to order ServerIdentity instances based on host name */
    private static class ServerIdentityComparator implements Comparator<ServerIdentity> {

        private static final ServerIdentityComparator INSTANCE = new ServerIdentityComparator();

        @Override
        public int compare(ServerIdentity o1, ServerIdentity o2) {
            int val = o1.getHostName().compareTo(o2.getHostName());
            if (val == 0) {
                val = o1.getServerName().compareTo(o2.getServerName());
            }
            return val;
        }

    }
}
