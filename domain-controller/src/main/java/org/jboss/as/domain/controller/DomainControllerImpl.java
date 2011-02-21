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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.controller.AbstractModelController;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ControllerTransaction;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.TransactionalModelController;
import org.jboss.dmr.ModelNode;
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
        READ_ONLY_OPERATIONS = Collections.unmodifiableSet(roops);
    }

    private final Map<String, TransactionalModelController> hosts = new ConcurrentHashMap<String, TransactionalModelController>();
    private final String localHostName;
    private final DomainModel localDomainModel;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MasterDomainControllerClient masterDomainControllerClient;

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.hosts.put(hostName, domainModel);
        this.masterDomainControllerClient = null;
    }

    public DomainControllerImpl(final ScheduledExecutorService scheduledExecutorService, final DomainModel domainModel, final String hostName, final MasterDomainControllerClient masterDomainControllerClient) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.masterDomainControllerClient = masterDomainControllerClient;
        this.localHostName = hostName;
        this.localDomainModel = domainModel;
        this.hosts.put(hostName, domainModel);
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
            ModelNode rsp = localDomainModel.execute(operation);
            if (!rsp.hasDefined(OUTCOME) || !SUCCESS.equals(rsp.get(OUTCOME))) {
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
    public OperationResult execute(ModelNode operation, ResultHandler handler) {

        // See who handles this op
        OperationRouting routing = determineRouting(operation);
        if (routing.isRouteToMaster()) {
            return masterDomainControllerClient.execute(operation, handler);
        }
        else if (routing.isLocalOnly()) {
            // It's either for a domain-level resource, which the DomainModel will handle directly,
            // or it's for a host resource, which the DomainModel will proxy off to the local HostController
            return localDomainModel.execute(operation, handler);
        }

        // Else we are responsible for coordinating a multi-host op

        // Push to hosts, formulate plan, push to servers
        ControllerTransaction  transaction = new ControllerTransaction();
        Map<String, ModelNode> hostResults = pushToHosts(operation, routing, transaction);
        for (ModelNode hostResult : hostResults.values()) {
            if (hostResult.hasDefined(OUTCOME) && "failed".equals(hostResult.get(OUTCOME).asString())) {
                transaction.setRollbackOnly();
                break;
            }
        }

        if (transaction.isRollbackOnly()) {
            transaction.commit();
            throw new UnsupportedOperationException("implement reporting of domain-level failure");
        } else {
            transaction.commit();
            // Formulate plan

            // Push to servers (via hosts)

            // Rollback if necessary

//            throw new UnsupportedOperationException("implement formulating and implementing a multi-server plan");
            handler.handleResultComplete();
            return new BasicOperationResult();
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
        else if (isReadOnly(operation)) {
            // Direct read of domain model
            routing = new OperationRouting(false);
        }
        else if ("composite".equals(operation.require(OP).asString())){
            // Recurse into the steps to see what's required
            if (operation.hasDefined("steps")) {
                Set<String> allHosts = new HashSet<String>();
                boolean routeToMaster = false;
                boolean hasLocal = false;
                for (ModelNode step : operation.get("steps").asList()) {
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

    private boolean isReadOnly(ModelNode operation) {
        // FIXME this is an overly primitive way to check for read-only ops
        boolean ro = READ_ONLY_OPERATIONS.contains(operation.require(OP).asString());
        if (!ro) {

        }
        return ro;
    }

    @Override
    public ModelNode getDomainModel() {
        return localDomainModel.getDomainModel();
    }

    private Map<String, ModelNode> pushToHosts(final ModelNode operation, final OperationRouting routing, final ControllerTransaction transaction) {
        final Map<String, ModelNode> hostResults = new HashMap<String, ModelNode>();
        final Map<String, Future<OperationResult>> futures = new HashMap<String, Future<OperationResult>>();

        // Try and execute locally first; if it fails don't bother with the other hosts
        final Set<String> targets = routing.getHosts();
        if (targets.remove(localHostName)) {
            pushToHost(operation, transaction, localHostName, hostResults, futures);
            processHostFuture(localHostName, futures.remove(localHostName), hostResults);
            ModelNode hostResult = hostResults.get(localHostName);
            if (!transaction.isRollbackOnly()) {
                if (hostResult.hasDefined(OUTCOME) && "failed".equals(hostResult.get(OUTCOME).asString())) {
                    transaction.setRollbackOnly();
                }
            }
        }

        if (!transaction.isRollbackOnly()) {
            for (final String host : targets) {
                pushToHost(operation, transaction, host, hostResults, futures);
            }

            log.debugf("Domain updates pushed to %s host controller(s)", futures.size());

            for (final Map.Entry<String, Future<OperationResult>> entry : futures.entrySet()) {
                String host = entry.getKey();
                Future<OperationResult> future = entry.getValue();
                processHostFuture(host, future, hostResults);
            }
        }

        return hostResults;
    }

    private void processHostFuture(String host, Future<OperationResult> future, final Map<String, ModelNode> hostResults) {
        try {
            OperationResult res = future.get();
            ModelNode compOp = res.getCompensatingOperation();
            hostResults.get(host).get(COMPENSATING_OPERATION).set(compOp == null ? new ModelNode() : compOp);
        } catch (final InterruptedException e) {
            log.debug("Interrupted reading host controller response");
            Thread.currentThread().interrupt();
            hostResults.put(host, getDomainFailureResult(e));
        } catch (final ExecutionException e) {
            log.debug("Execution exception reading host controller response", e);
            hostResults.put(host, getDomainFailureResult(e.getCause()));
        }
    }

    private void pushToHost(final ModelNode operation, final ControllerTransaction transaction, final String host,
            final Map<String, ModelNode> hostResults, final Map<String, Future<OperationResult>> futures) {
        final TransactionalModelController client = hosts.get(host);
        if (client != null) {
            final ModelNode hostResult = new ModelNode();
            hostResults.put(host, hostResult);
            final ResultHandler handler = new ResultHandler() {

                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                    hostResult.get(location).set(result);
                }

                @Override
                public void handleResultComplete() {
                    hostResult.get(OUTCOME).set("success");
                }

                @Override
                public void handleFailed(ModelNode failureDescription) {
                    hostResult.get(OUTCOME).set("failed");
                }

                @Override
                public void handleCancellation() {
                    hostResult.get(OUTCOME).set("cancelled");
                }

            };
            final Callable<OperationResult> callable = new Callable<OperationResult>() {

                @Override
                public OperationResult call() throws Exception {
                    return client.execute(operation, handler, transaction);
                }

            };

            futures.put(host, scheduledExecutorService.submit(callable));
        }
    }
    private ModelNode getDomainFailureResult(Throwable e) {
        ModelNode node = new ModelNode();
        node.get("outcome").set("failure");
        node.get("failure-description").set(getFailureResult(e));
        return node;
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

    @Override
    public void setInitialDomainModel(ModelNode initialModel) {
        if (masterDomainControllerClient == null) {
            throw new IllegalStateException("Cannot set initial domain model on non-slave DomainController");
        }
        // FIXME cast is poor
        ((DomainModelImpl) localDomainModel).setInitialDomainModel(initialModel);
    }
}
