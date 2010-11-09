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
import java.util.HashSet;
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
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.api.deployment.DeploymentAction;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.api.deployment.DeploymentSetPlan;
import org.jboss.as.domain.client.api.deployment.IncompleteDeploymentReplaceException;
import org.jboss.as.domain.client.api.deployment.InvalidDeploymentPlanException;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlan;
import org.jboss.as.domain.client.impl.DomainClientProtocol;
import org.jboss.as.domain.client.impl.DomainUpdateApplierResponse;
import org.jboss.as.domain.client.impl.deployment.DeploymentActionImpl;
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
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.logging.Logger;

/**
 * Handles the DomainControllerImpl's execution of a {@link DeploymentPlan}.
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
                boolean failed = false;
                try {
                    runDeploymentPlan(plan, responseQueue);
                    logger.infof("Completed deployment plan %s", plan.getId());
                }
                catch (InterruptedException e) {
                    failed = true;
                    Thread.currentThread().interrupt();
                    logger.errorf(e, "Interrupted while executing deployment plan %s", plan.getId());
                }
                catch (Exception e) {
                    logger.errorf(e, "Caught exception executing deployment plan %s", plan.getId());
                    failed = true;
                }
                catch (Error e) {
                    logger.errorf(e, "Caught error executing deployment plan %s", plan.getId());
                    throw e;
                }

                if (failed) {
                    try {
                        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_COMPLETE, null, true));
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };

        getDeploymentExecutor().submit(r);
    }

    /**
     * The actual deployment plan execution logic
     * @throws InterruptedException
     */
    private void runDeploymentPlan(final DeploymentPlan plan, final BlockingQueue<List<StreamedResponse>> responseQueue) throws InterruptedException {

        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_ID, plan.getId()));

        List<DeploymentSetPlan> setPlans = plan.getDeploymentSetPlans();
        List<DeploymentSetUpdates> updateSets = new ArrayList<DeploymentSetUpdates>(setPlans.size());
        for (DeploymentSetPlan setPlan : setPlans) {
            try {
                updateSets.add(createDeploymentSetUpdates(setPlan, getDomainModel(), setPlan.getDeploymentActions()));
            } catch (InvalidDeploymentPlanException e) {
                logger.errorf(e, "Deployment plan %s is invalid", plan.getId());
                pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_INVALID, e, true));
                return;
            }
        }

        List<DeploymentSetUpdates> rollbackSets = new ArrayList<DeploymentSetUpdates>(setPlans.size());
        boolean ok = true;
        boolean rollbackFailed = false;
        for (DeploymentSetUpdates updateSet : updateSets) {
            if (ok) {
                try {
                    ok = executeDeploymentSet(updateSet, responseQueue);
                    if (ok) {
                        rollbackSets.add(0, updateSet); // roll back in reverse order
                    }
                    else {
                        logger.debugf("Deployment set %s did not succeed", updateSet.setPlan.getId());
                    }
                }
                catch (RollbackFailedException e) {
                    // Deployment set execution failed and it also failed to roll itself back
                    ok = false;
                    rollbackFailed = true;
                    logger.errorf("Rollback of deployment set %s did not succeed", updateSet.setPlan.getId());
                }
            } else {
                // A previous set failed; just inform client this set is cancelled
                cancelDeploymentSet(updateSet, false, responseQueue);
            }
        }

        if (plan.isGlobalRollback()) {
            if (!ok && !rollbackFailed) {
                // Rollback the sets that succeeded before the one that failed
                // The one that failed will have rolled itself back.
                for (Iterator<DeploymentSetUpdates> iter = rollbackSets.iterator(); iter.hasNext();) {
                    DeploymentSetUpdates updateSet = iter.next();
                    try {
                        rollbackDeploymentSet(updateSet, responseQueue);
                        iter.remove();
                    }
                    catch (RollbackFailedException e) {
                        rollbackFailed = true;
                        logger.errorf("Rollback of deployment set %s did not succeed", updateSet.setPlan.getId());
                        // Don't try further rollbacks
                        break;
                    }
                }

            }

            if (rollbackFailed) {
                // Any remaining members in rollbackSets are there because rollback
                // of another set failed. So send notifications
                for (DeploymentSetUpdates updateSet : rollbackSets) {
                    cancelDeploymentSet(updateSet, true, responseQueue);
                }
            }
        }

        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_PLAN_COMPLETE, null, true));
    }

    private DomainModel getDomainModel() {
        return domainController.getDomainModel();
    }

    private boolean executeDeploymentSet(final DeploymentSetUpdates updateSet,
            final BlockingQueue<List<StreamedResponse>> responseQueue) throws InterruptedException, RollbackFailedException {

        logger.debugf("Executing deployment set %s", updateSet.setPlan.getId());

        // Execute domain model update on domain controller and server managers
        List<DomainUpdateApplierResponse> rsps = domainController.applyUpdatesToModel(updateSet.getDomainUpdates());

        // Inform client of results
        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID, updateSet.setPlan.getId()));
        DeploymentAction lastResponseAction = null;
        for (int i = 0; i < rsps.size(); i++) {
            DomainUpdateApplierResponse duar = rsps.get(i);
            // There can be multiple domain updates for a given action, but we
            // only send one response. Use this update result for the response if
            // 1) it failed or 2) it's the last update associated with the action
            if (duar.getDomainFailure() != null || duar.getHostFailures().size() > 0 || updateSet.isLastDomainUpdateForAction(i)) {
                DeploymentAction action = updateSet.getDeploymentActionForDomainUpdate(i);
                if (action != lastResponseAction) {
                    List<StreamedResponse> rspList = new ArrayList<StreamedResponse>(2);
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID, action.getId()));
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT, duar));
                    responseQueue.put(rspList);
                    lastResponseAction = action;
                }
            }
        }

        // See if the above was successful before moving on to servers
        DomainUpdateApplierResponse last = rsps.get(rsps.size() - 1);
        if (last.getDomainFailure() != null || last.getHostFailures().size() > 0) {
            // DomainModel update failed; don't apply to servers. The DomainControllerImpl will
            // have already rolled back the domain model update
            return false;
        }

        // Apply to servers
        Runnable r = getServerUpdateTask(updateSet, rsps, false, responseQueue);
        r.run();

        boolean ok = true;
        for (ServerUpdatePolicy policy : updateSet.updatePolicies.values()) {
            if (policy.isFailed()) {
                logger.infof("Deployment set failed on %s", policy.getServerGroupName());
                ok = false;
                break;
            }
        }

        if (!ok) {
            rollbackDeploymentSet(updateSet, responseQueue);
        }
        return ok;
    }

    private ExecutorService getDeploymentExecutor() {
        return executorService;
    }

    private void cancelDeploymentSet(final DeploymentSetUpdates updateSet,
                                     final boolean forRollback,
                                     final BlockingQueue<List<StreamedResponse>> responseQueue) throws InterruptedException {

        byte type = forRollback ? (byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ROLLBACK
                                : (byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ID;
        pushSingleResponse(responseQueue, new StreamedResponse(type, updateSet.setPlan.getId()));

        List<StreamedResponse> rspList = new ArrayList<StreamedResponse>();
        DomainUpdateApplierResponse duar = new DomainUpdateApplierResponse(true);
        for (ActionUpdates au : updateSet.actionUpdates) {
            rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID, au.action.getId()));
            rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT, duar));
        }

        responseQueue.put(rspList);
    }

    private void rollbackDeploymentSet(final DeploymentSetUpdates updateSet,
            final BlockingQueue<List<StreamedResponse>> responseQueue) throws InterruptedException, RollbackFailedException {

        logger.debugf("Rolling back deployment set %s", updateSet.setPlan.getId());

        // Execute domain model update on domain controller and server managers
        List<DomainUpdateApplierResponse> rsps = domainController.applyUpdatesToModel(updateSet.getDomainRollbacks());

        // Inform client of results
        pushSingleResponse(responseQueue, new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_SET_ROLLBACK, updateSet.setPlan.getId()));
        DeploymentAction lastResponseAction = null;
        for (int i = 0; i < rsps.size(); i++) {
            DomainUpdateApplierResponse duar = rsps.get(i);
            // There can be multiple domain updates for a given action, but we
            // only send one response. Use this update result for the response if
            // 1) it failed or 2) it's the last update associated with the action
            if (duar.getDomainFailure() != null || duar.getHostFailures().size() > 0 || updateSet.isLastDomainRollbackForAction(i)) {
                DeploymentAction action = updateSet.getDeploymentActionForDomainUpdate(i);
                if (action != lastResponseAction) {
                    List<StreamedResponse> rspList = new ArrayList<StreamedResponse>(2);
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_ID, action.getId()));
                    rspList.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_DEPLOYMENT_ACTION_MODEL_RESULT, duar));
                    responseQueue.put(rspList);
                    lastResponseAction = action;
                }
            }
        }

        DomainUpdateApplierResponse last = rsps.get(rsps.size() - 1);
        if (last.getDomainFailure() != null || last.getHostFailures().size() > 0) {
            throw new RollbackFailedException();
        }

        // Apply to servers
        Runnable r = getServerUpdateTask(updateSet, rsps, true, responseQueue);
        r.run();

        for (ServerUpdatePolicy policy : updateSet.rollbackPolicies.values()) {
            if (policy.isFailed()) {
                throw new RollbackFailedException();
            }
        }
    }

    private Runnable getServerUpdateTask(final DeploymentSetUpdates updateSet,
            final List<DomainUpdateApplierResponse> rsps,
            final boolean forRollbacks,
            final BlockingQueue<List<StreamedResponse>> responseQueue) {

        logger.debugf("Creating server tasks for %s domain responses", rsps.size());
        // Organize all the impacted servers by ServerGroup, sorted within a group by ServerManager
        Map<String, SortedSet<ServerIdentity>> serversByGroup = new HashMap<String, SortedSet<ServerIdentity>>();
        for (DomainUpdateApplierResponse duar : rsps) {
            for (ServerIdentity serverId : duar.getServers()) {
                String serverGroupName = serverId.getServerGroupName();
                SortedSet<ServerIdentity> set = serversByGroup.get(serverGroupName);
                if (set == null) {
                    logger.debugf("Found affected servers in server group %s", serverGroupName);
                    set = new TreeSet<ServerIdentity>(ServerIdentityComparator.INSTANCE);
                    serversByGroup.put(serverGroupName, set);
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

            Set<String> groupNames = new HashSet<String>(groupPlans.size());
            for (ServerGroupDeploymentPlan groupPlan : groupPlans) {
                groupNames.add(groupPlan.getServerGroupName());
            }
            List<Runnable> concurrentGroupsList = new ArrayList<Runnable>(groupPlans.size());
            ConcurrentGroupServerUpdatePolicy parent = new ConcurrentGroupServerUpdatePolicy(predecessor, groupNames);
            predecessor = parent;

            for (ServerGroupDeploymentPlan groupPlan : groupPlans) {
                String serverGroupName = groupPlan.getServerGroupName();
                SortedSet<ServerIdentity> servers = serversByGroup.get(serverGroupName);
                if (servers == null) {
                    // Just KISS and use a placeholder
                    servers = new TreeSet<ServerIdentity>();
                }

                ServerUpdatePolicy policy = null;
                if (forRollbacks) {
                    policy = new ServerUpdatePolicy(parent, serverGroupName, servers);
                    updateSet.rollbackPolicies.put(serverGroupName, policy);
                }
                else {
                    int maxFailures;
                    if (groupPlan.getMaxServerFailurePercentage() > 0) {
                        maxFailures = ((servers.size() * groupPlan.getMaxServerFailurePercentage()) / 100);
                    }
                    else {
                        maxFailures = groupPlan.getMaxServerFailures();
                    }
                    policy = new ServerUpdatePolicy(parent, serverGroupName, servers, maxFailures);
                    updateSet.updatePolicies.put(serverGroupName, policy);
                }

                List<Runnable> groupTasks = new ArrayList<Runnable>(servers.size());
                if (shutdown) {
                    for (ServerIdentity server : servers) {
                        groupTasks.add (new ServerRestartTask(server, updateSet, forRollbacks, policy, responseQueue, gracefulTimeout));
                    }
                }
                else if (forRollbacks) {
                    // need rollback tasks for this server
                    for (ServerIdentity server : servers) {
                        List<AbstractServerModelUpdate<?>> serverRollbacks = null;

                        // Find out what happened first time
                        List<UpdateResultHandlerResponse<?>> origResults  = updateSet.serverResults.get(server);
                        if (origResults == null) {
                            // Must be a new server that appeared. Assume it got
                            // the invalid model on start; roll 'em all back
                            serverRollbacks = updateSet.getServerRollbacks();
                        }
                        else {
                            serverRollbacks = new ArrayList<AbstractServerModelUpdate<?>>();
                            boolean rollingBack = false;
                            for (int i = origResults.size() - 1; i >= 0; i--) {
                                UpdateResultHandlerResponse<?> origResult = origResults.get(i);
                                if (!rollingBack) {
                                    // See if we need to roll this one back. Once we hit one in the list,
                                    // the rest need to be rolled back as well
                                    rollingBack = !origResult.isCancelled() && !origResult.isRolledBack();
                                }

                                if (rollingBack) {
                                    serverRollbacks.add(updateSet.getRollbackUpdateForServerUpdate(i));
                                }
                            }
                        }
                        if (serverRollbacks.size() > 0) {
                            groupTasks.add (new RunningServerUpdateTask(server, updateSet, serverRollbacks, policy, responseQueue));
                        }
                    }
                }
                else {
                    // Standard case
                    for (ServerIdentity server : servers) {
                        groupTasks.add (new RunningServerUpdateTask(server, updateSet, policy, responseQueue, groupPlan.isRollback()));
                    }
                }

                if (groupPlan.isRollingToServers()) {
                    concurrentGroupsList.add(new RollingUpdateTask(groupTasks));
                }
                else {
                    concurrentGroupsList.add(new ConcurrentUpdateTask(groupTasks, getDeploymentExecutor()));
                }
            }
            masterList.add(new ConcurrentUpdateTask(concurrentGroupsList, getDeploymentExecutor()));
        }

        return new RollingUpdateTask(masterList);
    }

    private static void pushSingleResponse(BlockingQueue<List<StreamedResponse>> queue, StreamedResponse response) throws InterruptedException {
        queue.put(Collections.singletonList(response));
    }

    /** Performs the translation from DeploymentAction to domain and server model updates */
    private static DeploymentSetUpdates createDeploymentSetUpdates(DeploymentSetPlan plan, DomainModel model, List<DeploymentAction> actions) throws InvalidDeploymentPlanException {

        logger.debugf("Creating DeploymentSetUpdates for deployment set %s", plan.getId());

        if (actions.size() == 0) {
            throw new InvalidDeploymentPlanException(String.format("%s %s contains no deployment actions", DeploymentSetPlan.class.getSimpleName(), plan.getId()));
        }

        List<ActionUpdates> actionUpdates = new ArrayList<ActionUpdates>();

        for (DeploymentAction action : actions) {
            ActionUpdates au = new ActionUpdates(action);
            actionUpdates.add(au);

            try {
                DeploymentActionImpl dai = (DeploymentActionImpl) action;
                switch (action.getType()) {
                    case ADD: {
                        String deploymentName = dai.getDeploymentUnitUniqueName();
                        logger.tracef("Add of deployment %s", deploymentName);
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
                            DomainDeploymentAdd dda = new DomainDeploymentAdd(deploymentName, runtimeName, hash, false);
                            au.domainUpdates.add(new DomainUpdate(dda, model, true));
                        }
                        // Now add to serve groups
                        ServerGroupDeploymentAdd sgda = new ServerGroupDeploymentAdd(deploymentName, runtimeName, hash, false);
                        addServerGroupUpdates(plan, au, sgda, model);
                        break;
                    }
                    case DEPLOY: {
                        logger.tracef("Deploy of deployment %s", dai.getDeploymentUnitUniqueName());
                        ServerGroupDeploymentStartStopUpdate sgdssu = new ServerGroupDeploymentStartStopUpdate(dai.getDeploymentUnitUniqueName(), true);
                        addServerGroupUpdates(plan, au, sgdssu, model);
                        break;
                    }
                    case FULL_REPLACE: {
                        logger.tracef("Full replace of deployment %s", dai.getDeploymentUnitUniqueName());
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
                        DomainDeploymentFullReplaceUpdate ddfru = new DomainDeploymentFullReplaceUpdate(deploymentName, dai.getNewContentFileName(), dai.getNewContentHash(), start);
                        au.domainUpdates.add(new DomainUpdate(ddfru, model, true));
                        break;
                    }
                    case REDEPLOY: {
                        logger.tracef("Redeploy of deployment %s", dai.getDeploymentUnitUniqueName());
                        DomainDeploymentRedeployUpdate ddru = new DomainDeploymentRedeployUpdate(dai.getDeploymentUnitUniqueName());
                        au.domainUpdates.add(new DomainUpdate(ddru, model, true));
                        break;
                    }
                    case REMOVE: {
                        logger.tracef("Remove of deployment %s", dai.getDeploymentUnitUniqueName());
                        ServerGroupDeploymentRemove sgdr = new ServerGroupDeploymentRemove(dai.getDeploymentUnitUniqueName());
                        addServerGroupUpdates(plan, au, sgdr, model);
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
                            DomainDeploymentRemove ddr = new DomainDeploymentRemove(dai.getDeploymentUnitUniqueName());
                            au.domainUpdates.add(new DomainUpdate(ddr, model, true));
                        }
                        break;
                    }
                    case REPLACE: {
                        logger.tracef("Replace of deployment %s", dai.getDeploymentUnitUniqueName());
                        ServerGroupDeploymentReplaceUpdate sgdru = new ServerGroupDeploymentReplaceUpdate(dai.getDeploymentUnitUniqueName(), dai.getNewContentFileName(), dai.getNewContentHash(), dai.getReplacedDeploymentUnitUniqueName());
                        addServerGroupUpdates(plan, au, sgdru, model);
                        break;
                    }
                    case UNDEPLOY: {
                        logger.tracef("Undeploy of deployment %s", dai.getDeploymentUnitUniqueName());
                        ServerGroupDeploymentStartStopUpdate sgdssu = new ServerGroupDeploymentStartStopUpdate(dai.getDeploymentUnitUniqueName(), false);
                        addServerGroupUpdates(plan, au, sgdssu, model);
                        break;
                    }
                    default:
                        throw new IllegalStateException(String.format("Unknown %s %s", DeploymentAction.class.getSimpleName(), action.getType()));
                }
            }
            catch (InvalidDeploymentPlanException e) {
                throw e;
            }
            catch (Exception e) {
                throw new InvalidDeploymentPlanException(String.format("Deployment action %s of type %s primarily affecting deployment %s is invalid", action.getId(), action.getType(), action.getDeploymentUnitUniqueName()), e);
            }
        }

        logger.debugf("Created %s action updates", actionUpdates.size());

        return new DeploymentSetUpdates(actionUpdates, plan);
    }

    private static void addServerGroupUpdates(final DeploymentSetPlan plan, final ActionUpdates au, final AbstractModelUpdate<ServerGroupElement, ?> serverGroupUpdate, DomainModel model) {
        boolean setSMU = true;
        for (Set<ServerGroupDeploymentPlan> ssgp : plan.getServerGroupDeploymentPlans()) {
            for (ServerGroupDeploymentPlan sgdp : ssgp) {
                AbstractDomainModelUpdate<?> dmu = DomainServerGroupUpdate.create(sgdp.getServerGroupName(), serverGroupUpdate);
                DomainUpdate du = new DomainUpdate(dmu, model, setSMU);
                au.domainUpdates.add(du);
                setSMU = setSMU && du.serverUpdate == null;
            }
        }
    }

    /** Holder for information about a deployment set */
    private static class DeploymentSetUpdates {
        private final List<ActionUpdates> actionUpdates;
        private final DeploymentSetPlan setPlan;
        private final Map<ServerIdentity, List<UpdateResultHandlerResponse<?>>> serverResults = new ConcurrentHashMap<ServerIdentity, List<UpdateResultHandlerResponse<?>>>();
        private final Map<String, ServerUpdatePolicy> updatePolicies = new HashMap<String, ServerUpdatePolicy>();;
        private final Map<String, ServerUpdatePolicy> rollbackPolicies = new HashMap<String, ServerUpdatePolicy>();;

        private DeploymentSetUpdates(final List<ActionUpdates> actionUpdates, final DeploymentSetPlan setPlan) {
            this.actionUpdates = actionUpdates;
            this.setPlan = setPlan;
        }

        private List<AbstractDomainModelUpdate<?>> getDomainUpdates() {
            List<AbstractDomainModelUpdate<?>>result = new ArrayList<AbstractDomainModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                for (DomainUpdate du : au.domainUpdates) {
                    result.add(du.update);
                }
            }
            return result;
        }

        private List<AbstractServerModelUpdate<?>> getServerUpdates() {
            List<AbstractServerModelUpdate<?>>result = new ArrayList<AbstractServerModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null) {
                        result.add(du.serverUpdate.update);
                    }
                }
            }
            return result;
        }

        private List<AbstractDomainModelUpdate<?>> getDomainRollbacks() {
            List<AbstractDomainModelUpdate<?>>result = new ArrayList<AbstractDomainModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.compensatingUpdate != null)
                    result.add(du.compensatingUpdate);
                }
            }
            return result;
        }

        private List<AbstractServerModelUpdate<?>> getServerRollbacks() {
            List<AbstractServerModelUpdate<?>>result = new ArrayList<AbstractServerModelUpdate<?>>();
            for (ActionUpdates au : actionUpdates) {
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null) {
                        if (du.serverUpdate.compensatingUpdate != null) {
                            result.add(du.serverUpdate.compensatingUpdate);
                        }
                    }
                }
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

        private boolean isLastDomainRollbackForAction(int index) {
            int count = 0;
            for (int i = actionUpdates.size() -1; i >= 0; i--) {
                ActionUpdates au = actionUpdates.get(i);
                int rbCount = 0;
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.compensatingUpdate != null)
                        rbCount++;
                }
                count += rbCount;
                if (index < count)
                    return (index == count - 1);
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last domain rollback update (" + (getDomainRollbacks().size() - 1) + ")");
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
                int suCount = 0;
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null)
                        suCount++;
                }
                count += suCount;
                if (index < count)
                    return (index == count - 1);
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last server update (" + (getServerUpdates().size() - 1) + ")");
        }

        private boolean isLastServerRollbackUpdateForAction(int index) {
            int count = 0;
            for (int i = actionUpdates.size() -1; i >= 0; i--) {
                ActionUpdates au = actionUpdates.get(i);
                int suCount = 0;
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null && du.serverUpdate.compensatingUpdate != null)
                        suCount++;
                }
                count += suCount;
                if (index < count)
                    return (index == count - 1);
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last server rollback update (" + (getServerRollbacks().size() - 1) + ")");
        }

        private DeploymentAction getDeploymentActionForServerUpdate(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                int suCount = 0;
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null)
                        suCount++;
                }
                count += suCount;
                if (index < count) {
                    return au.action;
                }
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last server update (" + (getServerUpdates().size() - 1) + ")");
        }

        private DeploymentAction getDeploymentActionForServerRollbackUpdate(int index) {
            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                int suCount = 0;
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null && du.serverUpdate.compensatingUpdate != null)
                        suCount++;
                }
                count += suCount;
                if (index < count) {
                    return au.action;
                }
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last server update (" + (getServerUpdates().size() - 1) + ")");
        }

        private AbstractServerModelUpdate<?> getRollbackUpdateForServerUpdate(int index) {

            int count = 0;
            for (ActionUpdates au : actionUpdates) {
                for (DomainUpdate du : au.domainUpdates) {
                    if (du.serverUpdate != null) {
                        if (index == count++) {
                            return du.serverUpdate.compensatingUpdate;
                        }
                    }
                }
            }
            throw new IndexOutOfBoundsException(index + " is larger than the index of the last domain update (" + (getServerUpdates().size() - 1) + ")");
        }
    }

    /** Simple data class to associate an action with the relevant update objects */
    private static class ActionUpdates {
        private final DeploymentAction action;
        private final List<DomainUpdate> domainUpdates = new ArrayList<DomainUpdate>();

        private ActionUpdates(DeploymentAction action) {
            this.action = action;
        }
    }

    private abstract class AbstractServerUpdateTask implements Runnable {
        protected final ServerUpdatePolicy updatePolicy;
        protected final ServerIdentity serverId;
        protected final DeploymentSetUpdates updates;
        protected final BlockingQueue<List<StreamedResponse>> responseQueue;
        protected final boolean forRollback;

        AbstractServerUpdateTask(final ServerIdentity serverId, final DeploymentSetUpdates updates,
                final boolean forRollback, final ServerUpdatePolicy updatePolicy,
                final BlockingQueue<List<StreamedResponse>> responseQueue) {
            this.serverId = serverId;
            this.updatePolicy = updatePolicy;
            this.responseQueue = responseQueue;
            this.updates = updates;
            this.forRollback = forRollback;
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

            byte type = forRollback ? (byte) DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_ROLLBACK
                                    : (byte) DomainClientProtocol.RETURN_SERVER_DEPLOYMENT;
            responses.add(new StreamedResponse(type, actionId));

            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_HOST_NAME, serverId.getHostName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_GROUP_NAME, serverId.getServerGroupName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_NAME, serverId.getServerName()));
            responses.add(new StreamedResponse((byte) DomainClientProtocol.RETURN_SERVER_DEPLOYMENT_RESULT, urhr));
            try {
                responseQueue.put(responses);
            } catch (InterruptedException e) {
                logger.errorf("%s interrupted sending server update responses for %s %s", toString(), DeploymentSetPlan.class.getSimpleName(), updates.setPlan.getId());
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
        private final List<AbstractServerModelUpdate<?>> serverUpdates;

        /**
         * Constructor for the normal case
         */
        private RunningServerUpdateTask(final ServerIdentity serverId,
                final DeploymentSetUpdates updates,
                final ServerUpdatePolicy updatePolicy,
                final BlockingQueue<List<StreamedResponse>> responseQueue,
                final boolean allowOverallRollback) {
            super(serverId, updates, false, updatePolicy, responseQueue);
            this.allowOverallRollback = allowOverallRollback;
            this.serverUpdates = updates.getServerUpdates();
        }

        /**
         * Constructor for the rollback case
         */
        private RunningServerUpdateTask(final ServerIdentity serverId,
                final DeploymentSetUpdates updates,
                final List<AbstractServerModelUpdate<?>> serverUpdates,
                final ServerUpdatePolicy updatePolicy,
                final BlockingQueue<List<StreamedResponse>> responseQueue) {
            super(serverId, updates, true, updatePolicy, responseQueue);
            this.allowOverallRollback = false;
            this.serverUpdates = serverUpdates;
        }

        @Override
        protected void processUpdates() {
            try {
                logger.debugf("Applying %s updates to  %s", updates.getServerUpdates().size(), serverId);
                List<UpdateResultHandlerResponse<?>> rsps =
                    domainController.applyUpdatesToServer(serverId, serverUpdates, allowOverallRollback);
                if (!forRollback) {
                    updates.serverResults.put(serverId, rsps);
                }
                updatePolicy.recordServerResult(serverId, rsps);

                // Push responses to client
                DeploymentAction lastResponseAction = null;
                for (int i = 0; i < rsps.size(); i++) {
                    UpdateResultHandlerResponse<?> urhr = rsps.get(i);
                    // There can be multiple server updates for a given action, but we
                    // only send one response. Use this update result for the response if
                    // 1) it failed or 2) it's the last update associated with the action
                    boolean sendResponse = urhr.getFailureResult() != null;
                    if (!sendResponse) {
                        sendResponse = forRollback ? updates.isLastServerRollbackUpdateForAction(i)
                                                   : updates.isLastServerUpdateForAction(i);
                    }
                    if (sendResponse) {
                        DeploymentAction action = forRollback ? updates.getDeploymentActionForServerRollbackUpdate(i)
                                                              : updates.getDeploymentActionForServerUpdate(i);
                        if (action != lastResponseAction) {
                            sendServerUpdateResult(action.getId(), urhr);
                            lastResponseAction = action;
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.errorf(e, "Caught exception applying updates to %s", serverId);
            }
        }
    }

    /** Restarts a server */
    private class ServerRestartTask extends AbstractServerUpdateTask {

        private final long gracefulTimeout;

        private ServerRestartTask(final ServerIdentity serverId, final DeploymentSetUpdates updates,
                final boolean forRollback, final ServerUpdatePolicy updatePolicy,
                final BlockingQueue<List<StreamedResponse>> responseQueue, final long gracefulTimeout) {
            super(serverId, updates, forRollback, updatePolicy, responseQueue);
            this.gracefulTimeout = gracefulTimeout;
        }

        @Override
        protected void processUpdates() {

            UpdateResultHandlerResponse<?> urhr;
            ServerStatus status = domainController.restartServer(serverId.getHostName(), serverId.getServerName(), gracefulTimeout);
            switch (status) {
                case STARTED:
                case STARTING:
                    urhr = UpdateResultHandlerResponse.createRestartResponse();
                default: {
                    UpdateFailedException ufe = new UpdateFailedException("Server " + serverId.getServerName() + " did not restart. Server status is " + status);
                    urhr = UpdateResultHandlerResponse.createFailureResponse(ufe);
                }
            }
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

    /** Exception indicating the rollback of a deployment set failed */
    private static class RollbackFailedException extends Exception {
        private static final long serialVersionUID = 3524620474555254562L;
    }

    /** Associates a domain update with its compensating update and its server update */
    private static class DomainUpdate {
        private final AbstractDomainModelUpdate<?> update;
        private final AbstractDomainModelUpdate<?> compensatingUpdate;
        private final ServerUpdate serverUpdate;

        private DomainUpdate(final AbstractDomainModelUpdate<?> update,
                             final DomainModel model,
                             final boolean createServerUpdate) {
            this.update = update;
            this.compensatingUpdate = update.getCompensatingUpdate(model);
            if (createServerUpdate) {
                AbstractServerModelUpdate<?> smu = update.getServerModelUpdate();
                if (smu != null) {
                    AbstractServerModelUpdate<?> csmu = compensatingUpdate == null ? null : compensatingUpdate.getServerModelUpdate();
                    serverUpdate = new ServerUpdate(smu, csmu);
                }
                else serverUpdate = null;
            }
            else serverUpdate = null;
        }
    }

    /** Associates a server update with its compensating update */
    private static class ServerUpdate {
        private final AbstractServerModelUpdate<?> update;
        private final AbstractServerModelUpdate<?> compensatingUpdate;

        private ServerUpdate(final AbstractServerModelUpdate<?> update,
                final AbstractServerModelUpdate<?> compensatingUpdate) {
            this.update = update;
            this.compensatingUpdate = compensatingUpdate;
        }
    }
}
