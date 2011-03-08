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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_FAILURE_DESCRIPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.domain.controller.plan.RolloutPlanController;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainControllerImpl extends AbstractModelController implements DomainControllerSlave {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

    private static final List<Property> EMPTY_ADDRESS = Collections.emptyList();

    // FIXME this is an overly primitive way to check for read-only ops
    private static final Set<String> READ_ONLY_OPERATIONS;
    static {
        Set<String> roops = new HashSet<String>();
        roops.add(READ_ATTRIBUTE_OPERATION);
        roops.add(READ_CHILDREN_NAMES_OPERATION);
        roops.add(READ_OPERATION_DESCRIPTION_OPERATION);
        roops.add(READ_OPERATION_NAMES_OPERATION);
        roops.add(READ_RESOURCE_DESCRIPTION_OPERATION);
        roops.add(READ_RESOURCE_OPERATION);
        roops.add(READ_CHILDREN_TYPES_OPERATION);
        roops.add(READ_CONFIG_AS_XML_OPERATION);
        READ_ONLY_OPERATIONS = Collections.unmodifiableSet(roops);
    }

    private final Map<String, HostControllerClient> hosts = new ConcurrentHashMap<String, HostControllerClient>();
    private final Map<String, HostControllerClient> immutableHosts = Collections.unmodifiableMap(hosts);
    private final String localHostName;
    private final DomainModel localDomainModel;
    private final ScheduledExecutorService scheduledExecutorService;
    private final FileRepository fileRepository;
    private final MasterDomainControllerClient masterDomainControllerClient;

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName, final FileRepository fileRepository) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.hosts.put(hostName, new LocalHostControllerClient());
        this.fileRepository = fileRepository;
        this.masterDomainControllerClient = null;

    }

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName, final FileRepository fileRepository,
            final MasterDomainControllerClient masterDomainControllerClient) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.masterDomainControllerClient = masterDomainControllerClient;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.fileRepository = new FallbackRepository(fileRepository, masterDomainControllerClient.getRemoteFileRepository());
        this.hosts.put(hostName, new LocalHostControllerClient());
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode addClient(final HostControllerClient client) {
        Logger.getLogger("org.jboss.domain").info("register host " + client.getId());
        this.hosts.put(client.getId(), client);
        return localDomainModel.getDomainModel();
    }

    /** {@inheritDoc} */
    @Override
    public void removeClient(final String id) {
        this.hosts.remove(id);
    }

    @Override
    public ModelNode getProfileOperations(String profileName) {
        ModelNode operation = new ModelNode();

        operation.get(OP).set(DESCRIBE);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, profileName)).toModelNode());

        try {
            ModelNode rsp = localDomainModel.execute(ExecutionContextBuilder.Factory.create(operation).build());
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


    @Override
    public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {

        ModelNode operation = executionContext.getOperation();
        // See who handles this op
        OperationRouting routing = determineRouting(operation);
        if (routing.isRouteToMaster()) {
            return masterDomainControllerClient.execute(executionContext, handler);
        }
        else if (routing.isLocalOnly()) {
            // It's either for a domain-level resource, which the DomainModel will handle directly,
            // or it's for a host resource, which the DomainModel will proxy off to the local HostController
            return localDomainModel.execute(executionContext, handler);
        }

        // Else we are responsible for coordinating a multi-host op

        // Get a copy of the rollout plan so it doesn't get disrupted by any handlers
        ModelNode rolloutPlan = operation.has(ROLLOUT_PLAN) ? operation.remove(ROLLOUT_PLAN) : null;

        // Push to hosts, formulate plan, push to servers
        ControllerTransaction  transaction = new ControllerTransaction();
        Map<String, ModelNode> hostResults = pushToHosts(executionContext, routing, transaction);
        ModelNode masterFailureResult = null;
        ModelNode hostFailureResults = null;
        ModelNode masterResult = hostResults.get(localHostName);
        if (masterResult.hasDefined(OUTCOME) && FAILED.equals(masterResult.get(OUTCOME).asString())) {
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

//        for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
//            System.out.println("======================================================");
//            System.out.println(entry.getKey());
//            System.out.println("======================================================");
//            System.out.println(entry.getValue());
//        }

        if (transaction.isRollbackOnly()) {
            transaction.commit();
            if (masterFailureResult != null) {
                handler.handleResultFragment(new String[]{DOMAIN_FAILURE_DESCRIPTION}, masterFailureResult);
            }
            else if (hostFailureResults != null) {
                handler.handleResultFragment(new String[]{HOST_FAILURE_DESCRIPTIONS}, hostFailureResults);
            }
            handler.handleFailed(null);
            return new BasicOperationResult();
        } else {
            // TODO formulate the domain-level result
            //....

            // Formulate plan
            Map<String, Map<ServerIdentity, ModelNode>> opsByGroup = getOpsByGroup(hostResults);
            try {
                rolloutPlan = getRolloutPlan(rolloutPlan, opsByGroup);
            }
            catch (OperationFailedException ofe) {
                transaction.setRollbackOnly();
                // treat as a master DC failure
                handler.handleResultFragment(new String[]{DOMAIN_FAILURE_DESCRIPTION}, ofe.getFailureDescription());
                handler.handleFailed(null);
                return new BasicOperationResult();
            }
            finally {
                transaction.commit();
            }

            ModelNode compensatingOperation = getCompensatingOperation(operation, hostResults);

            // Push to servers (via hosts)
            RolloutPlanController controller = new RolloutPlanController(opsByGroup, rolloutPlan, handler, immutableHosts, scheduledExecutorService);

            controller.execute();
            // Rollback if necessary

//            throw new UnsupportedOperationException("implement formulating and implementing a multi-server plan");
            handler.handleResultComplete();
            return new BasicOperationResult(compensatingOperation);
        }
    }

    private OperationRouting determineRouting(ModelNode operation) {
        OperationRouting routing;

        String targetHost = null;
        List<Property> address = operation.hasDefined(OP_ADDR) ? operation.get(OP_ADDR).asPropertyList() : EMPTY_ADDRESS;
        if (address.size() > 0) {
            Property first = address.get(0);
            if (HOST.equals(first.getName())) {
                targetHost = first.getValue().asString();
            }
        }

        if (localHostName.equals(targetHost)) {
            routing = new OperationRouting(false);
        }
        else if (masterDomainControllerClient != null) {
            // TODO a slave could conceivably handle locally read-only requests for the domain model
            routing = new OperationRouting(true);
        }
        else if (targetHost != null) {
            List<String> hosts = Collections.singletonList(targetHost);
            routing = new OperationRouting(hosts);
        }
        else if (isReadOnly(operation, address)) {
            // Direct read of domain model
            routing = new OperationRouting(false);
        }
        else if (COMPOSITE.equals(operation.require(OP).asString())){
            // Recurse into the steps to see what's required
            if (operation.hasDefined(STEPS)) {
                Set<String> allHosts = new HashSet<String>();
                boolean routeToMaster = false;
                boolean hasLocal = false;
                for (ModelNode step : operation.get(STEPS).asList()) {
                    OperationRouting stepRouting = determineRouting(step);
                    if (stepRouting.isRouteToMaster()) {
                        routeToMaster = true;
                        break;
                    }
                    else if (stepRouting.isLocalOnly()) {
                        hasLocal = true;
                    }
                    else {
                        allHosts.addAll(stepRouting.getHosts());
                    }
                }

                if (routeToMaster) {
                    routing = new OperationRouting(true);
                }
                else if (allHosts.size() == 0) {
                    routing = new OperationRouting(false);
                }
                else {
                    if (hasLocal) {
                        allHosts.add(localHostName);
                    }
                    routing = new OperationRouting(allHosts);
                }
            }
            else {
                // empty; this will be an error but don't deal with it here
                routing = new OperationRouting(false);
            }
        }
        else {
            // Write operation to the model; everyone gets it
            routing = new OperationRouting(this.hosts.keySet());
        }
        return routing;
    }

    private boolean isReadOnly(final ModelNode operation, final List<Property> address) {
        // FIXME this is an overly primitive way to check for read-only ops
        String opName = operation.require(OP).asString();
        boolean ro = READ_ONLY_OPERATIONS.contains(opName);
        if (!ro && address.size() == 1 && DESCRIBE.equals(opName) && PROFILE.equals(address.get(0).getName())) {
            ro = true;
        }
        return ro;
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

    private Map<String, ModelNode> pushToHosts(final ExecutionContext executionContext, final OperationRouting routing, final ControllerTransaction transaction) {
        final Map<String, ModelNode> hostResults = new HashMap<String, ModelNode>();
        final Map<String, Future<ModelNode>> futures = new HashMap<String, Future<ModelNode>>();

        ModelNode wrapper = new ModelNode();
        wrapper.get(OP).set(HostControllerClient.EXECUTE_ON_DOMAIN);
        wrapper.get(HostControllerClient.DOMAIN_OP).set(executionContext.getOperation());

        ExecutionContext wrapperContext = executionContext.clone(wrapper);
        // Try and execute locally first; if it fails don't bother with the other hosts
        final Set<String> targets = routing.getHosts();
        if (targets.remove(localHostName)) {
            if (targets.size() > 0) {
                // clone things so our local activity doesn't affect the op
                // we send to the other hosts
                wrapperContext = wrapperContext.clone(wrapper.clone());
            }
            pushToHost(wrapperContext, transaction, localHostName, futures);
            processHostFuture(localHostName, futures.remove(localHostName), hostResults);
            ModelNode hostResult = hostResults.get(localHostName);
            if (!transaction.isRollbackOnly()) {
                if (hostResult.hasDefined(OUTCOME) && FAILED.equals(hostResult.get(OUTCOME).asString())) {
                    transaction.setRollbackOnly();
                }
            }
        }

        if (!transaction.isRollbackOnly()) {
            for (final String host : targets) {
                pushToHost(wrapperContext, transaction, host, futures);
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

    private void pushToHost(final ExecutionContext executionContext, final ControllerTransaction transaction, final String host,
            final Map<String, Future<ModelNode>> futures) {
        final HostControllerClient client = hosts.get(host);
        if (client != null) {
            final Callable<ModelNode> callable = new Callable<ModelNode>() {

                @Override
                public ModelNode call() throws Exception {
                    return client.execute(executionContext, transaction);
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
                        for(Property prop : series.get(SERVER_GROUP).asPropertyList()) {
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
        if (found.add(prop.getName())) {
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

            SortedSet<String> keys = new TreeSet<String>(operation.get(STEPS).keys());
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
            if (compSteps.size() < keys.size()) {
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
            for (String step : keys) {
                ModelNode stepComp = compSteps.get(step);
                if (stepComp != null && stepComp.isDefined()) {
                    result.add(stepComp);
                }
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

    private class OperationRouting {

        private final boolean routeToMaster;
        private final boolean localOnly;
        private final Set<String> hosts;

        private OperationRouting(boolean routeToMaster) {
            this.routeToMaster = routeToMaster;
            this.localOnly = !routeToMaster;
            this.hosts = Collections.emptySet();
        }

        private OperationRouting(final Collection<String> hosts) {
            this.routeToMaster = false;
            this.localOnly = false;
            this.hosts = new HashSet<String>(hosts);
        }

        private boolean isRouteToMaster() {
            return routeToMaster;
        }

        private boolean isLocalOnly() {
            return localOnly;
        }

        private Set<String> getHosts() {
            return hosts;
        }
    }

    private class LocalHostControllerClient implements HostControllerClient {

        private final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(HOST, localHostName));
        @Override
        public PathAddress getProxyNodeAddress() {
            return address;
        }

        @Override
        public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
            return localDomainModel.execute(executionContext, handler);
        }

        @Override
        public ModelNode execute(ExecutionContext executionContext) throws CancellationException {
            return localDomainModel.execute(executionContext);
        }

        @Override
        public ModelNode execute(ExecutionContext executionContext, ControllerTransactionContext transaction) {
            return localDomainModel.execute(executionContext, transaction);
        }

        @Override
        public OperationResult execute(ExecutionContext executionContext, ResultHandler handler,
                ControllerTransactionContext transaction) {
            return localDomainModel.execute(executionContext, handler, transaction);
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
