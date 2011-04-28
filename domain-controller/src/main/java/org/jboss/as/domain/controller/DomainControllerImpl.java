/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_FAILURE_DESCRIPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CONFIG_AS_XML_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.controller.AbstractModelController;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ControllerTransaction;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadUtil;
import org.jboss.as.domain.controller.plan.RolloutPlanController;
import org.jboss.as.domain.controller.plan.ServerOperationExecutor;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * Standard {@link DomainController} implementation.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainControllerImpl extends AbstractModelController<Void> implements DomainController, DomainControllerSlave {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

    // FIXME this is an overly primitive way to check for read-only ops
    private static final Set<String> READ_ONLY_OPERATIONS;
    private static final Set<String> DEPLOYMENT_OPS;
    static {
        Set<String> roops = new HashSet<String>();
        roops.add(READ_ATTRIBUTE_OPERATION);
        roops.add(READ_CHILDREN_NAMES_OPERATION);
        roops.add(READ_OPERATION_DESCRIPTION_OPERATION);
        roops.add(READ_OPERATION_NAMES_OPERATION);
        roops.add(READ_RESOURCE_DESCRIPTION_OPERATION);
        roops.add(READ_RESOURCE_OPERATION);
        roops.add(READ_CHILDREN_TYPES_OPERATION);
        roops.add(READ_CHILDREN_RESOURCES_OPERATION);
        roops.add(READ_CONFIG_AS_XML_OPERATION);
        READ_ONLY_OPERATIONS = Collections.unmodifiableSet(roops);
        Set<String> deploymentOps = new HashSet<String>();
        deploymentOps.add(DeploymentUploadBytesHandler.OPERATION_NAME);
        deploymentOps.add(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME);
        deploymentOps.add(DeploymentUploadURLHandler.OPERATION_NAME);
        DEPLOYMENT_OPS = Collections.unmodifiableSet(deploymentOps);
    }

    private final Map<String, DomainControllerSlaveClient> hosts;
    private final String localHostName;
    private final DomainModel localDomainModel;
    private final ScheduledExecutorService scheduledExecutorService;
    private final FileRepository fileRepository;
    private final DeploymentRepository deploymentRepository;
    private final MasterDomainControllerClient masterDomainControllerClient;
    private final ServerOperationExecutor serverOperationExecutor = new ServerOperationExecutor() {
        @Override
        public ModelNode executeServerOperation(ServerIdentity server, Operation operation) {
            return executeOnHost(server.getHostName(), operation, (ControllerTransactionContext) null);
        }
    };

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName, final FileRepository fileRepository,
            DeploymentRepository deploymentRepository, Map<String, DomainControllerSlaveClient> hosts) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.hosts = hosts;
        this.hosts.put(hostName, new LocalDomainModelAdapter());
        this.fileRepository = fileRepository;
        this.deploymentRepository = deploymentRepository;
        this.masterDomainControllerClient = null;

    }

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName, final FileRepository fileRepository,
            final MasterDomainControllerClient masterDomainControllerClient, Map<String, DomainControllerSlaveClient> hosts) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.masterDomainControllerClient = masterDomainControllerClient;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.fileRepository = new FallbackRepository(fileRepository, masterDomainControllerClient.getRemoteFileRepository());
        this.hosts = hosts;
        this.hosts.put(hostName, new LocalDomainModelAdapter());
        this.deploymentRepository = null;
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode addClient(final DomainControllerSlaveClient client) {
        Logger.getLogger("org.jboss.domain").info("register host " + client.getId());
        if (hosts.containsKey(client.getId()) || localHostName.equals(client.getId())){
            throw new IllegalArgumentException("There is already a host named '" + client.getId() + "'");
        }
        this.hosts.put(client.getId(), client);
        return localDomainModel.getDomainModel();
    }

    /** {@inheritDoc} */
    @Override
    public void removeClient(final String id) {
        Logger.getLogger("org.jboss.domain").info("unregister host " + id);
        this.hosts.remove(id);
    }

    @Override
    public ModelNode getProfileOperations(String profileName) {
        ModelNode operation = new ModelNode();

        operation.get(OP).set(DESCRIBE);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, profileName)).toModelNode());

        try {
            ModelNode rsp = localDomainModel.execute(OperationBuilder.Factory.create(operation).build());
            if (!rsp.hasDefined(OUTCOME) || !SUCCESS.equals(rsp.get(OUTCOME).asString())) {
                ModelNode msgNode = rsp.get(FAILURE_DESCRIPTION);
                String msg = msgNode.isDefined() ? msgNode.toString() : "Failed to retrieve profile operations from domain controller";
                throw new RuntimeException(msg);
            }
            return rsp.require(RESULT);
        } catch (CancellationException e) {
            // AutoGenerated
            throw new RuntimeException(e);
        }
    }


    /**
     * Routes the request to {@link DomainModel#executeForDomain(Operation, ControllerTransactionContext)}
     */
    @Override
    public ModelNode execute(final Operation operation, final ControllerTransactionContext transaction) {
        return localDomainModel.executeForDomain(operation, transaction);
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler, final ControllerTransactionContext transaction) {
        throw new UnsupportedOperationException("Transactional operations with a passed in ResultHandler are not supported");
    }

    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler, Void handback) {

        // FIXME JBAS-9338 Prevent concurrent write requests
        try {
            ModelNode operationNode = operation.getOperation();

            // System.out.println("------ operation " + operation);

            // See who handles this op
            OperationRouting routing = determineRouting(operationNode);
            if ((routing.isRouteToMaster() || !routing.isLocalOnly()) && masterDomainControllerClient != null) {
                // System.out.println("------ route to master ");
                // Per discussion on 2011/03/07, routing requests from a slave to the
                // master may overly complicate the security infrastructure. Therefore,
                // the ability to do this is being disabled until it's clear that it's
                // not a problem
    //            return masterDomainControllerClient.execute(operationContext, handler);
                PathAddress addr = PathAddress.pathAddress(operationNode.get(OP_ADDR));
                handler.handleFailed(new ModelNode().set("Operations for address " + addr +
                        " can only handled by the master Domain Controller; this host is not the master Domain Controller"));
                return new BasicOperationResult();
            }
            else if (!routing.isTwoStep()) {
                // It's either for a read of a domain-level resource or it's for a single host-level resource,
                // either of which a single host client can handle directly
                String host = routing.getSingleHost();
                // System.out.println("------ route to host " + host);
                return executeOnHost(host, operation, handler);
            }
            else if (routing.getSingleHost() != null && !localHostName.equals(routing.getSingleHost())) {
                // Two step operation, but not coordinated by this host
                String host = routing.getSingleHost();
                // System.out.println("------ route two-step operation to host " + host);
                return executeOnHost(host, operation, handler);
            }
            else {
                // Else we are responsible for coordinating a two-phase op
                // -- apply to DomainController models across domain and then push to servers
                return executeTwoPhaseOperation(operation, handler, operationNode, routing);
            }
        } catch (OperationFailedException e) {
            log.debugf(e, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
            handler.handleFailed(e.getFailureDescription());
            return new BasicOperationResult();
        } catch (final Throwable t) {
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
        }
    }

    private OperationResult executeTwoPhaseOperation(final Operation operation, final ResultHandler handler,
            ModelNode operationNode, OperationRouting routing) throws OperationFailedException {

        DomainLevelResult domainResult = executeOnDomainControllers(operation, handler, routing);
        if(domainResult.singleHostResult != null) {
            // FIXME Reformat a single domain-result
            if(domainResult.singleHostResult.hasDefined(DOMAIN_RESULTS) && ! domainResult.singleHostResult.hasDefined(SERVER_OPERATIONS)) {
                final List<Property> steps = domainResult.singleHostResult.get(DOMAIN_RESULTS).asPropertyList();
                if(steps.size() == 1) {
                    final ModelNode fragment = domainResult.singleHostResult.get(DOMAIN_RESULTS).get("step-1");
                    handler.handleResultFragment(Util.NO_LOCATION, fragment);
                }
            }

            handler.handleResultComplete();
            return new BasicOperationResult(domainResult.compensatingOperation);
        }

        // Push to servers (via hosts)
        RolloutPlanController controller = new RolloutPlanController(domainResult.opsByGroup, domainResult.rolloutPlan, handler, serverOperationExecutor, scheduledExecutorService, false);
        RolloutPlanController.Result controllerResult = controller.execute();

        // Rollback if necessary
        switch (controllerResult) {
            case FAILED: {
                controller.rollback();
                handler.handleFailed(new ModelNode().set("Operation was not applied successfully to any servers"));
                return new BasicOperationResult(domainResult.compensatingOperation);
            }
            case PARTIAL: {
                controller.rollback();
                // fall through
            }
            case SUCCESS: {
                handler.handleResultComplete();
                return new BasicOperationResult(domainResult.compensatingOperation);
            }
            default:
                throw new IllegalStateException(String.format("Unknown %s %s", RolloutPlanController.Result.class.getCanonicalName(), controllerResult));
        }

    }

    private DomainLevelResult executeOnDomainControllers(Operation operation, ResultHandler handler, OperationRouting routing) throws OperationFailedException {
        ControllerTransaction  transaction = new ControllerTransaction();
        try {
            ModelNode operationNode = operation.getOperation();
            // Get a copy of the rollout plan so it doesn't get disrupted by any handlers
            ModelNode rolloutPlan = operationNode.hasDefined(OPERATION_HEADERS) && operationNode.get(OPERATION_HEADERS).has(ROLLOUT_PLAN)
                ? operationNode.get(OPERATION_HEADERS).remove(ROLLOUT_PLAN) : null;

            // System.out.println("---- Push to hosts");
            // Push to hosts, formulate plan, push to servers
            Map<String, ModelNode> hostResults = null;
            try {
                hostResults = pushToHosts(operation, routing, transaction);
            }
            catch (Exception e) {
                ModelNode failureMsg = new ModelNode();
                failureMsg.get(DOMAIN_FAILURE_DESCRIPTION).set(e.toString());
                throw new OperationFailedException(failureMsg);
            }

            // System.out.println("---- Pushed to hosts");
            ModelNode masterFailureResult = null;
            ModelNode hostFailureResults = null;
            ModelNode masterResult = hostResults.get(localHostName);
            // System.out.println("-----Checking host results");
            if (masterResult != null && masterResult.hasDefined(OUTCOME) && FAILED.equals(masterResult.get(OUTCOME).asString())) {
                transaction.setRollbackOnly();
                masterFailureResult = masterResult.hasDefined(FAILURE_DESCRIPTION) ? masterResult.get(FAILURE_DESCRIPTION) : new ModelNode().set("Unexplained failure");
            }
            else {
                for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
                    ModelNode hostResult = entry.getValue();
                    if (hostResult.hasDefined(OUTCOME) && FAILED.equals(hostResult.get(OUTCOME).asString())) {
                        if (hostFailureResults == null) {
                            transaction.setRollbackOnly();
                            hostFailureResults = new ModelNode();
                        }
                        ModelNode desc = hostResult.hasDefined(FAILURE_DESCRIPTION) ? hostResult.get(FAILURE_DESCRIPTION) : new ModelNode().set("Unexplained failure");
                        hostFailureResults.add(entry.getKey(), desc);
                    }
                }
            }

            // for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
            //    System.out.println("======================================================");
            //    System.out.println(entry.getKey());
            //    System.out.println("======================================================");
            //    System.out.println(entry.getValue());
            // }

            if (transaction.isRollbackOnly()) {
                ModelNode failureMsg = new ModelNode();
                if (masterFailureResult != null) {
                    failureMsg.get(DOMAIN_FAILURE_DESCRIPTION).set(masterFailureResult);
                }
                else if (hostFailureResults != null) {
                    failureMsg.get(HOST_FAILURE_DESCRIPTIONS).set(hostFailureResults);
                }
                throw new OperationFailedException(failureMsg);
            }

            // TODO formulate the domain-level result
            //....

            // Formulate plan
            Map<String, Map<ServerIdentity, ModelNode>> opsByGroup = getOpsByGroup(hostResults);
            ModelNode compensatingOperation = getCompensatingOperation(operationNode, hostResults);
            // System.out.println(compensatingOperation);

            DomainLevelResult result;
            if (opsByGroup.size() == 0) {
                ModelNode singleHostResult = getSingleHostResult(hostResults);
                result = new DomainLevelResult(singleHostResult, compensatingOperation);
            }
            else {
                try {
                    rolloutPlan = getRolloutPlan(rolloutPlan, opsByGroup);
                    // System.out.println(rolloutPlan);
                }
                catch (OperationFailedException ofe) {
                    // treat as a master DC failure
                    ModelNode failureMsg = new ModelNode();
                    failureMsg.get(DOMAIN_FAILURE_DESCRIPTION).set(ofe.getFailureDescription());
                    throw new OperationFailedException(failureMsg);
                }
                result = new DomainLevelResult(opsByGroup, compensatingOperation, rolloutPlan);
            }

            return result;

        } catch (OperationFailedException ofe) {
            transaction.setRollbackOnly();
            throw ofe;
        } finally {
            transaction.commit();
        }

    }

    @Override
    protected Void getOperationControllerContext(Operation operation) {
        return null;
    }

    private OperationRouting determineRouting(ModelNode operation) {
        OperationRouting routing;

        String targetHost = null;
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String operationName = operation.get(OP).asString();
        if (address.size() > 0) {
            PathElement first = address.getElement(0);
            if (HOST.equals(first.getKey())) {
                targetHost = first.getValue();
            }
        }

        if (targetHost != null) {
            routing = null;
            if(isReadOnly(operation, address)) {
                routing =  new OperationRouting(targetHost, false);
            }
            // Check if the target is an actual server
            else if(address.size() > 1) {
                PathElement first = address.getElement(1);
                if (SERVER.equals(first.getKey())) {
                    routing =  new OperationRouting(targetHost, false);
                }
            }
            if (routing == null) {
                if("start".equals(operationName) || "stop".equals(operationName) || "restart".equals(operationName)) {
                    routing = new OperationRouting(targetHost, false);
                } else {
                    routing = new OperationRouting(targetHost, true);
                }
            }
        }
        else if (masterDomainControllerClient != null) {
            // TODO a slave could conceivably handle locally read-only requests for the domain model
            routing = new OperationRouting();
        }
        else if (isReadOnly(operation, address)) {
            // Direct read of domain model
            routing = new OperationRouting(localHostName, false);
        } else if (DEPLOYMENT_OPS.contains(operationName)) {
            // Deployment ops should be executed on the master DC only
            routing = new OperationRouting(localHostName, false);
        } else if (COMPOSITE.equals(operationName)){
            // Recurse into the steps to see what's required
            if (operation.hasDefined(STEPS)) {
                Set<String> allHosts = new HashSet<String>();
                boolean twoStep = false;
                for (ModelNode step : operation.get(STEPS).asList()) {
                    OperationRouting stepRouting = determineRouting(step);
                    if (stepRouting.isTwoStep()) {
                        twoStep = true;
                    }
                    allHosts.addAll(stepRouting.getHosts());
                }

                if (allHosts.size() == 1) {
                    routing = new OperationRouting(allHosts.iterator().next(), twoStep);
                }
                else {
                    routing = new OperationRouting(allHosts);
                }
            }
            else {
                // empty; this will be an error but don't deal with it here
                // Let our DomainModel deal with it
                routing = new OperationRouting(localHostName, false);
            }
        }
        else {
            // Write operation to the model; everyone gets it
            routing = new OperationRouting(this.hosts.keySet());
        }
        return routing;
    }

    private boolean isReadOnly(final ModelNode operation, final PathAddress address) {
        // FIXME this is an overly primitive way to check for read-only ops
        String opName = operation.require(OP).asString();
        boolean ro = READ_ONLY_OPERATIONS.contains(opName);
        if (!ro && address.size() == 1 && DESCRIBE.equals(opName) && PROFILE.equals(address.getElement(0).getKey())) {
            ro = true;
        }
        return ro;
    }

    @Override
    public ModelNode getDomainAndHostModel() {
        return ((DomainModelImpl) localDomainModel).getDomainAndHostModel();
    }

    @Override
    public ModelNode getDomainModel() {
        return localDomainModel.getDomainModel();
    }

    @Override
    public FileRepository getFileRepository() {
        return fileRepository;
    }

    @Override
    public void setInitialDomainModel(ModelNode initialModel) {
        if (masterDomainControllerClient == null) {
            throw new IllegalStateException("Cannot set initial domain model on non-slave DomainController");
        }
        // FIXME cast is poor
        ((DomainModelImpl) localDomainModel).setInitialDomainModel(initialModel);
    }

    private Map<String, ModelNode> pushToHosts(Operation operation, final OperationRouting routing,
            final ControllerTransaction transaction) throws Exception {

        final Map<String, ModelNode> hostResults = new HashMap<String, ModelNode>();
        final Map<String, Future<ModelNode>> futures = new HashMap<String, Future<ModelNode>>();
        ModelNode opNode = operation.getOperation();
        // Try and execute locally first; if it fails don't bother with the other hosts
        final Set<String> targets = routing.getHosts();
        if (targets.remove(localHostName)) {

            if (deploymentRepository != null) {
                // Store any uploaded content now
                storeDeploymentContent(operation, opNode);
            }

            Operation localOperation = operation;
            if (targets.size() > 0) {
                // clone things so our local activity doesn't affect the op
                // we send to the other hosts
                localOperation = localOperation.clone(localOperation.getOperation().clone());
            }
            pushToHost(localOperation, transaction, localHostName, futures);
            processHostFuture(localHostName, futures.remove(localHostName), hostResults);
            ModelNode hostResult = hostResults.get(localHostName);
            if (!transaction.isRollbackOnly()) {
                if (hostResult.hasDefined(OUTCOME) && FAILED.equals(hostResult.get(OUTCOME).asString())) {
                    transaction.setRollbackOnly();
                }
            }
        }

        if (!transaction.isRollbackOnly()) {

            // We don't push stream to slaves
            operation = OperationBuilder.Factory.create(opNode).build();

            for (final String host : targets) {
                pushToHost(operation, transaction, host, futures);
            }

            log.debugf("Domain updates pushed to %s host controller(s)", futures.size());

            for (final Map.Entry<String, Future<ModelNode>> entry : futures.entrySet()) {
                String host = entry.getKey();
                Future<ModelNode> future = entry.getValue();
                processHostFuture(host, future, hostResults);
            }
        }

        return hostResults;
    }

    private void storeDeploymentContent(Operation operation, ModelNode opNode) throws Exception {
        // A pretty painful hack. We analyze the operation for operations that include deployment content attachments; if found
        // we store the content and replace the attachments

        PathAddress address = PathAddress.pathAddress(opNode.get(OP_ADDR));
        if (address.size() == 0) {
            String opName = opNode.get(OP).asString();
            if (DeploymentFullReplaceHandler.OPERATION_NAME.equals(opName) && opNode.hasDefined(INPUT_STREAM_INDEX)) {
                byte[] hash = DeploymentUploadUtil.storeDeploymentContent(operation, opNode, deploymentRepository);
                opNode.remove(INPUT_STREAM_INDEX);
                opNode.get(HASH).set(hash);
            }
            else if (COMPOSITE.equals(opName) && opNode.hasDefined(STEPS)){
                // Check the steps
                for (ModelNode childOp : opNode.get(STEPS).asList()) {
                    storeDeploymentContent(operation, childOp);
                }
            }
        }
        else if (address.size() == 1 && DEPLOYMENT.equals(address.getElement(0).getKey())
                && ADD.equals(opNode.get(OP).asString()) && opNode.hasDefined(INPUT_STREAM_INDEX)) {
            byte[] hash = DeploymentUploadUtil.storeDeploymentContent(operation, opNode, deploymentRepository);
            opNode.remove(INPUT_STREAM_INDEX);
            opNode.get(HASH).set(hash);
        }
    }

    private void processHostFuture(String host, Future<ModelNode> future, final Map<String, ModelNode> hostResults) {
        try {
            hostResults.put(host, future.get());
        } catch (final InterruptedException e) {
            log.debug("Interrupted reading host controller response");
            Thread.currentThread().interrupt();
            hostResults.put(host, getDomainFailureResult(e));
        } catch (final ExecutionException e) {
            log.info("Execution exception reading host controller response", e);
            hostResults.put(host, getDomainFailureResult(e.getCause()));
        }
    }

    private void pushToHost(final Operation operation, final ControllerTransaction transaction, final String host,
            final Map<String, Future<ModelNode>> futures) {
        if (hosts.containsKey(host)) {
            final Callable<ModelNode> callable = new Callable<ModelNode>() {

                @Override
                public ModelNode call() throws Exception {
                    try {
                        //System.out.println("------ pushing to host " + host);
                        ModelNode node = executeOnHost(host, operation, transaction);
                        //System.out.println("---- host result " + node);
                        return node;
                    } finally {
                        //System.out.println("------ pushed to host " + host);
                    }
                }
            };

            futures.put(host, scheduledExecutorService.submit(callable));
        }
    }

    private ModelNode getDomainFailureResult(Throwable e) {
        ModelNode node = new ModelNode();
        node.get(OUTCOME).set(FAILED);
        node.get(FAILURE_DESCRIPTION).set(getFailureResult(e));
        return node;
    }


    private Map<String, Map<ServerIdentity, ModelNode>> getOpsByGroup(Map<String, ModelNode> hostResults) {
        Map<String, Map<ServerIdentity, ModelNode>> result = new HashMap<String, Map<ServerIdentity, ModelNode>>();

        for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
            ModelNode hostResult = entry.getValue().get(RESULT);
            if (hostResult.hasDefined(SERVER_OPERATIONS)) {
                String host = entry.getKey();
                for (ModelNode item : hostResult.get(SERVER_OPERATIONS).asList()) {
                    ModelNode op = item.require(OP);
                    for (Property prop : item.require(SERVERS).asPropertyList()) {
                        String group = prop.getValue().asString();
                        Map<ServerIdentity, ModelNode> groupMap = result.get(group);
                        if (groupMap == null) {
                            groupMap = new HashMap<ServerIdentity, ModelNode>();
                            result.put(group, groupMap);
                        }
                        groupMap.put(new ServerIdentity(host, group, prop.getName()), op);
                    }
                }
            }
        }
        return result;
    }

    private ModelNode getSingleHostResult(Map<String, ModelNode> hostResults) {
        ModelNode singleHost = hostResults.get(localHostName);
        if (singleHost != null
                && (!singleHost.hasDefined(OUTCOME) || IGNORED.equals(singleHost.get(OUTCOME).asString()))) {
            singleHost = null;
        }
        if (singleHost == null) {
            for (ModelNode node : hostResults.values()) {
                if (node.hasDefined(OUTCOME) && !IGNORED.equals(node.get(OUTCOME).asString())) {
                    singleHost = node;
                    break;
                }
            }
        }

        return singleHost == null ? new ModelNode() : singleHost.get(RESULT);
    }

    private ModelNode getRolloutPlan(ModelNode rolloutPlan, Map<String, Map<ServerIdentity, ModelNode>> opsByGroup) throws OperationFailedException {

        if (rolloutPlan == null || !rolloutPlan.isDefined()) {
            rolloutPlan = getDefaultRolloutPlan(opsByGroup);
        }
        else {
            // Validate that plan covers all groups
            Set<String> found = new HashSet<String>();
            if (rolloutPlan.hasDefined(IN_SERIES)) {
                for (ModelNode series : rolloutPlan.get(IN_SERIES).asList()) {
                    if (series.hasDefined(CONCURRENT_GROUPS)) {
                        for(Property prop : series.get(CONCURRENT_GROUPS).asPropertyList()) {
                            validateServerGroupPlan(found, prop);
                        }
                    }
                    else if (series.hasDefined(SERVER_GROUP)) {
                        Property prop = series.get(SERVER_GROUP).asProperty();
                        validateServerGroupPlan(found, prop);
                    }
                    else {
                        throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. %s is not a valid child of node %s", series, IN_SERIES)));
                    }
                }
            }

            Set<String> groups = new HashSet<String>(opsByGroup.keySet());
            groups.removeAll(found);
            if (!groups.isEmpty()) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Plan operations affect server groups %s that are not reflected in the rollout plan", groups)));
            }
        }
        return rolloutPlan;
    }

    private void validateServerGroupPlan(Set<String> found, Property prop) throws OperationFailedException {
        if (!found.add(prop.getName())) {
            throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s appears more than once in the plan.", prop.getName())));
        }
        ModelNode plan = prop.getValue();
        if (plan.hasDefined(MAX_FAILURE_PERCENTAGE)) {
            if (plan.has(MAX_FAILED_SERVERS)) {
                plan.remove(MAX_FAILED_SERVERS);
            }
            int max = plan.get(MAX_FAILURE_PERCENTAGE).asInt();
            if (max < 0 || max > 100) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s has a %s value of %s; must be between 0 and 100.", prop.getName(), MAX_FAILURE_PERCENTAGE, max)));
            }
        }
        if (plan.hasDefined(MAX_FAILED_SERVERS)) {
            int max = plan.get(MAX_FAILED_SERVERS).asInt();
            if (max < 0) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s has a %s value of %s; cannot be less than 0.", prop.getName(), MAX_FAILED_SERVERS, max)));
            }
        }
    }

    private ModelNode getDefaultRolloutPlan(Map<String, Map<ServerIdentity, ModelNode>> opsByGroup) {
        ModelNode result = new ModelNode();
        if (opsByGroup.size() > 0) {
            ModelNode groups = result.get(IN_SERIES).add().get(CONCURRENT_GROUPS);

            ModelNode groupPlan = new ModelNode();
            groupPlan.get(ROLLING_TO_SERVERS).set(false);
            groupPlan.get(MAX_FAILED_SERVERS).set(0);

            for (String group : opsByGroup.keySet()) {
                groups.add(group, groupPlan);
            }
            result.get(ROLLBACK_ACROSS_GROUPS).set(true);
        }
        return result;
    }

    private ModelNode getCompensatingOperation(ModelNode operation, Map<String, ModelNode> hostResults) {

        ModelNode result = null;
        if (isMultistepOperation(operation)) {

            int stepCount = operation.get(STEPS).asInt();
            Map<String, ModelNode> compSteps = new HashMap<String, ModelNode>();
            // See if master responded; if yes use that for all possible steps
            ModelNode masterResult = hostResults.get(localHostName);
            if (masterResult != null && !IGNORED.equals(masterResult.get(OUTCOME).asString())
                    && masterResult.hasDefined(COMPENSATING_OPERATION)) {
                for (Property prop : masterResult.get(COMPENSATING_OPERATION).asPropertyList()) {
                    ModelNode value = prop.getValue();
                    if (value.getType() == ModelType.OBJECT) {
                        compSteps.put(prop.getName(), value);
                    }
                }
            }
            if (compSteps.size() < stepCount) {
                // See if other hosts handled other steps
                for (ModelNode hostResult : hostResults.values()) {
                    if (hostResult != null && !IGNORED.equals(hostResult.get(OUTCOME).asString())
                            && hostResult.hasDefined(COMPENSATING_OPERATION)) {

                        for (Property prop : masterResult.get(COMPENSATING_OPERATION).asPropertyList()) {
                            if (!compSteps.containsKey(prop.getName())) {
                                ModelNode value = prop.getValue();
                                if (value.getType() == ModelType.OBJECT) {
                                    compSteps.put(prop.getName(), value);
                                }
                            }
                        }
                    }
                }
            }

            result = new ModelNode();
            for (int i = 1; i <= stepCount; i++) {
                String step = "step-" + i;
                ModelNode stepComp = compSteps.get(step);
                if (stepComp != null && stepComp.isDefined()) {
                    result.get("steps").add(stepComp);
                }
            }
            if (result.isDefined()) {
                result.get(OP).set(COMPOSITE);
            }
        }
        else {
            // See if master responded; if yes use that; if not use first host that responded
            ModelNode masterResult = hostResults.get(localHostName);
            if (masterResult != null && !IGNORED.equals(masterResult.get(OUTCOME).asString())) {
                result = masterResult.get(COMPENSATING_OPERATION);
            }
            if (result == null) {
                for (ModelNode hostResult : hostResults.values()) {
                    if (hostResult != null && !IGNORED.equals(hostResult.get(OUTCOME).asString())) {
                        result = hostResult.get(COMPENSATING_OPERATION);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean isMultistepOperation(ModelNode operation) {
        // TODO deal with wildcard ops
        return COMPOSITE.equals(operation.require(OP).asString());
    }

    private ModelNode executeOnHost(String hostName, Operation operation, ControllerTransactionContext tx) {
        DomainControllerSlaveClient client = hosts.get(hostName);
        if (client == null) {
            // TODO deal with host disappearance
            return null;
        }
        else if (hostName.equals(localHostName)) {
            return tx == null ? client.execute(operation) : client.execute(operation, tx);
        }
        else {
            // Prevent concurrent use of the client connection until we move to remoting
            synchronized (client) {
                return tx == null ? client.execute(operation) : client.execute(operation, tx);
            }
        }
    }

    private OperationResult executeOnHost(String hostName, Operation operation, ResultHandler handler) {
        DomainControllerSlaveClient client = hosts.get(hostName);
        if (client == null) {
            handler.handleFailed(new ModelNode().set("No host called " + hostName + " exists"));
            return new BasicOperationResult();
        }
        else if (hostName.equals(localHostName)) {
            return client.execute(operation, handler);
        }
        else {
            // Prevent concurrent use of the client connection until we move to remoting
            synchronized (client) {
                return client.execute(operation, handler);
            }
        }
    }

    private class OperationRouting {

        private final Set<String> hosts = new HashSet<String>();
        private final boolean twoStep;
        private final boolean routeToMaster;

        /** Constructor for domain-level requests where we are not master */
        private OperationRouting() {
            twoStep = false;
            routeToMaster = true;
        }

        /**
         * Constructor for a request routed to a single host
         *
         * @param host the name of the host
         * @param twoStep true if a two-step execution is needed
         */
        private OperationRouting(String host, boolean twoStep) {
            this.hosts.add(host);
            this.twoStep = twoStep;
            routeToMaster = false;
        }

        /**
         * Constructor for a request routed to multiple hosts
         *
         * @param hosts the names of the hosts
         */
        private OperationRouting(final Collection<String> hosts) {
            this.hosts.addAll(hosts);
            this.twoStep = true;
            routeToMaster = false;
        }

        private Set<String> getHosts() {
            return hosts;
        }

        private String getSingleHost() {
            return hosts.size() == 1 ? hosts.iterator().next() : null;
        }

        private boolean isTwoStep() {
            return twoStep;
        }

        private boolean isRouteToMaster() {
            return routeToMaster;
        }

        private boolean isLocalOnly() {
            return localHostName.equals(getSingleHost());
        }
    }

    private class DomainLevelResult {
        private final ModelNode compensatingOperation;
        private final ModelNode rolloutPlan;
        private final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup;
        private final ModelNode singleHostResult;

        private DomainLevelResult(final ModelNode singleHostResult, final ModelNode compensatingOperation) {
            this.singleHostResult = singleHostResult;
            this.compensatingOperation = compensatingOperation;
            this.rolloutPlan = null;
            this.opsByGroup = null;
        }

        private DomainLevelResult(final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup,
                final ModelNode compensatingOperation, final ModelNode rolloutPlan) {
            this.opsByGroup = opsByGroup;
            this.compensatingOperation = compensatingOperation;
            this.rolloutPlan = rolloutPlan;
            this.singleHostResult = null;
        }
    }

    /**
     * Adapter to allow this domain controller to talk to the DomainModel via the same
     * interface it uses for remote slave domain controllers.
     */
    private class LocalDomainModelAdapter implements DomainControllerSlaveClient {

        @Override
        public OperationResult execute(Operation operation, ResultHandler handler) {
            return localDomainModel.execute(operation, handler);
        }

        @Override
        public ModelNode execute(Operation operation) throws CancellationException {
            return localDomainModel.execute(operation);
        }

        @Override
        public ModelNode execute(Operation operation, ControllerTransactionContext transaction) {
            return localDomainModel.executeForDomain(operation, transaction);
        }

        @Override
        public OperationResult execute(Operation operation, ResultHandler handler,
                ControllerTransactionContext transaction) {
            throw new UnsupportedOperationException("Transactional operations with a passed in ResultHandler are not supported");
        }

        @Override
        public String getId() {
            return localHostName;
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }
}
