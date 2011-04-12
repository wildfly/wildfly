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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ControllerResource;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelProvider;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContextFactory;
import org.jboss.as.controller.OperationControllerContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SynchronousOperationSupport;
import org.jboss.as.controller.BasicModelController.XmlMarshallingHandler;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.descriptions.common.ExtensionDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersister.SnapshotInfo;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelImpl extends BasicModelController implements DomainModel {

    private final ExtensionContext extensionContext;
    private final ServerOperationResolver serverOperationResolver;
    private final String localHostName;
    private final ModelNode hostModel;
    private final Map<String, DomainControllerSlaveClient> hosts;
    private final ExtensibleConfigurationPersister injectedHostPersister;
    private final boolean master;
    private final ConfigurationPersister delegatingHostPersister = new ConfigurationPersister() {

        @Override
        public void store(ModelNode model) throws ConfigurationPersistenceException {
            // We'll be given the whole model but we only persist the host part
            injectedHostPersister.store(model.get(HOST, localHostName));
        }

        @Override
        public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
            injectedHostPersister.marshallAsXml(model, output);
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            throw new UnsupportedOperationException("load() should not be called as part of operation handling");
        }

        @Override
        public void successfulBoot() throws ConfigurationPersistenceException {
        }

        @Override
        public String snapshot() {
            return null;
        }

        @Override
        public SnapshotInfo listSnapshots() {
            return NULL_SNAPSHOT_INFO;
        }

        @Override
        public void deleteSnapshot(String name) {
        }

    };
    private final ConfigurationPersisterProvider hostPersisterProvider = new ConfigurationPersisterProvider() {
        @Override
        public ConfigurationPersister getConfigurationPersister() {
            return delegatingHostPersister;
        }
    };

    /** Constructor for a master DC. */
    protected DomainModelImpl(final ExtensibleConfigurationPersister configurationPersister, final LocalHostModel localHostProxy,
            final DeploymentRepository deploymentRepo, final FileRepository fileRepository, final Map<String, DomainControllerSlaveClient> hosts) {
        this(null, configurationPersister, localHostProxy, deploymentRepo, fileRepository, hosts, true);
    }

    /** Constructor for a slave DC. */
    protected DomainModelImpl(final ModelNode model, final ExtensibleConfigurationPersister configurationPersister, final LocalHostModel localHostProxy,
            final DeploymentRepository deploymentRepo, final FileRepository fileRepository, final Map<String, DomainControllerSlaveClient> hosts) {
        this(model, configurationPersister, localHostProxy, deploymentRepo, fileRepository, hosts, false);
    }

    protected DomainModelImpl(final ModelNode model, final ExtensibleConfigurationPersister configurationPersister, final LocalHostModel localHostProxy,
            final DeploymentRepository deploymentRepo, final FileRepository fileRepository, final Map<String, DomainControllerSlaveClient> hosts, final boolean master) {

        super(getInitialModel(model), configurationPersister, DomainDescriptionProviders.ROOT_PROVIDER);
        this.localHostName = localHostProxy.getName();

        ModelNodeRegistration registry = getRegistry();
        if (model == null) {
            this.extensionContext = DomainModelUtil.initializeMasterDomainRegistry(registry, configurationPersister, deploymentRepo, fileRepository, this);
        }
        else {
            this.extensionContext = DomainModelUtil.initializeSlaveDomainRegistry(registry, configurationPersister, deploymentRepo, fileRepository, this);
        }
        registerInternalOperations();

        ModelNodeRegistration hostRegistry = localHostProxy.getRegistry();
        registry.registerSubModel(PathElement.pathElement(HOST, localHostName), hostRegistry);
        this.hostModel = localHostProxy.getHostModel();
        XmlMarshallingHandler xmlHandler = new XmlMarshallingHandler(localHostProxy.getConfigurationPersister(), hostModel);
        hostRegistry.registerOperationHandler(CommonDescriptions.READ_CONFIG_AS_XML, xmlHandler, xmlHandler, false, OperationEntry.EntryType.PRIVATE);

        this.injectedHostPersister = localHostProxy.getConfigurationPersister();
        ModelNode ourModel = getModel();
        ourModel.get(HOST, localHostName).set(this.hostModel);
        this.serverOperationResolver = new ServerOperationResolver(localHostName);
        if (model == null) {
            initializeExtensions(ourModel);
        }
        this.hosts = Collections.unmodifiableMap(hosts);
        this.master = master;
    }

    private static ModelNode getInitialModel(final ModelNode model) {
        return model == null ? DomainModelUtil.createCoreModel() : model;
    }

    public boolean isMaster() {
        return master;
    }

    public ModelNode getDomainAndHostModel() {
        return super.getModel().clone();
    }

    @Override
    public ModelNode getDomainModel() {
        ModelNode model = super.getModel().clone();
        // trim off the host model
        model.get(HOST).set(new ModelNode());
        return model;
    }

    // FIXME the domainModel really should not expose hosts
    public Set<String> getHostNames() {
        return new HashSet<String>(hosts.keySet());
    }

    // FIXME the domainModel really should not expose hosts
    public Map<String, DomainControllerSlaveClient> getRemoteHosts(){
        if (hosts.size() == 1 && hosts.containsKey(localHostName)) {
            return Collections.emptyMap();
        }
        Map<String, DomainControllerSlaveClient> hosts = new HashMap<String, DomainControllerSlaveClient>(this.hosts);
        hosts.remove(localHostName);
        return hosts;
    }

    @Override
    protected OperationControllerContext getOperationControllerContext(Operation operation) {
        return getOperationControllerContext(operation, null);
    }

    protected OperationControllerContext getOperationControllerContext(final Operation operation, final ControllerTransactionContext transaction) {
        boolean forHost = isOperationForHost(operation.getOperation());
        final ConfigurationPersisterProvider persisterProvider = new DualRootConfigurationPersisterProvider(getConfigurationPersisterProvider(), hostPersisterProvider, forHost);
        return new OperationControllerContext() {

            @Override
            public ModelProvider getModelProvider() {
                return DomainModelImpl.this.getModelProvider();
            }

            @Override
            public OperationContextFactory getOperationContextFactory() {
                return DomainModelImpl.this.getOperationContextFactory();
            }

            @Override
            public ConfigurationPersisterProvider getConfigurationPersisterProvider() {
                return persisterProvider;
            }

            @Override
            public ControllerTransactionContext getControllerTransactionContext() {
                return transaction;
            }
        };
    }


    @Override
    protected OperationResult doExecute(final OperationContext operationHandlerContext, final Operation operation,
            final OperationHandler operationHandler, final ResultHandler resultHandler,
            final PathAddress address, final OperationControllerContext operationControllerContext) throws OperationFailedException {

        ControllerTransactionContext transaction = operationControllerContext.getControllerTransactionContext();
        if (transaction == null) {
            return super.doExecute(operationHandlerContext, operation, operationHandler, resultHandler, address, operationControllerContext);
        }

        try {
            ModelNode opNode = operation.getOperation();
            final OperationResult result = operationHandler.execute(operationHandlerContext, opNode, resultHandler);
            ControllerResource txResource = getControllerResource(operationHandlerContext, opNode, operationHandler, resultHandler,
                                                                  address, operationControllerContext);
            if (txResource != null) {
                transaction.registerResource(txResource);
            }
            return result;
        } catch (OperationFailedException e) {
            transaction.setRollbackOnly();
            throw e;
        }
    }

    @Override
    protected MultiStepOperationController getMultiStepOperationController(Operation executionContext, ResultHandler handler,
            final OperationControllerContext operationControllerContext) throws OperationFailedException {

        DualRootConfigurationPersisterProvider confPerstProvider =
            (DualRootConfigurationPersisterProvider) operationControllerContext.getConfigurationPersisterProvider();
        return new TransactionalMultiStepOperationController(executionContext, handler, operationControllerContext.getModelProvider(),
                confPerstProvider, operationControllerContext.getControllerTransactionContext());
    }

    protected ControllerResource getControllerResource(final OperationContext context, final ModelNode operation, final OperationHandler operationHandler,
            final ResultHandler resultHandler, final PathAddress address, final OperationControllerContext operationControllerContext) {
        ControllerResource resource = null;

        if (operationHandler instanceof ModelUpdateOperationHandler) {
            resource = new DomainModelControllerResource(operationHandler, address, context.getSubModel(), operationControllerContext);
        }

        return resource;
    }

    private void initializeExtensions(ModelNode model) {
        // If we were provided a model, we're a slave and need to initialize all extensions
        if (model != null && model.hasDefined(EXTENSION)) {
            for (Property prop : model.get(EXTENSION).asPropertyList()) {
                try {
                    String module = prop.getValue().get(ExtensionDescription.MODULE).asString();
                    for (Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                        extension.initialize(extensionContext);
                    }
                } catch (ModuleLoadException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    void setInitialDomainModel(ModelNode domainModel) {
        ModelNode root = getModel();
        // Preserve the "host" subtree
        ModelNode host = root.get(HOST);
        root.set(domainModel);
        root.get(HOST).set(host);
        // Now we know what extensions are needed
        initializeExtensions(domainModel);
    }

    private static boolean isOperationForHost(ModelNode operation) {
        if (operation.hasDefined(OP_ADDR)) {
            ModelNode address = operation.get(OP_ADDR);
            if (address.asInt() > 0 && HOST.equals(address.get(0).asProperty().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ModelNode executeForDomain(final Operation operation, final ControllerTransactionContext transaction) {
        ModelNode op = operation.getOperation();
        ParsedOp parsedOp = parseOperation(op, 0);
        ModelNode domainOp = parsedOp.getDomainOperation();
        ModelNode overallResult = null;
        if (domainOp != null) {
            DelegatingControllerTransactionContext delegateTx = new DelegatingControllerTransactionContext(transaction);
            ModelNode opResult = SynchronousOperationSupport.execute(operation.clone(domainOp), getOperationControllerContext(operation, delegateTx), this);
            overallResult = createOverallResult(opResult, parsedOp, delegateTx);
        }
        else {
            overallResult = new ModelNode();
            overallResult.get(OUTCOME).set(IGNORED);
        }
        return overallResult;
    }

    private ParsedOp parseOperation(ModelNode operation, int index) {
        String targetHost = null;
        String runningServerTarget = null;
        ModelNode runningServerOp = null;
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (address.size() > 0) {
            PathElement first = address.getElement(0);
            if (HOST.equals(first.getKey())) {
                targetHost = first.getValue();
                if (address.size() > 1 && RUNNING_SERVER.equals(address.getElement(1).getKey())) {
                    runningServerTarget = address.getElement(1).getValue();
                    ModelNode relativeAddress = new ModelNode();
                    for (int i = 2; i < address.size(); i++) {
                        PathElement element = address.getElement(i);
                        relativeAddress.add(element.getKey(), element.getValue());
                    }
                    runningServerOp = operation.clone();
                    runningServerOp.get(OP_ADDR).set(relativeAddress);
                }
            }
        }

        ParsedOp result = null;

        if (targetHost != null && !localHostName.equals(targetHost)) {
            result = new SimpleParsedOp(index);
        }
        else if (runningServerTarget != null) {
            result = new SimpleParsedOp(index, runningServerTarget, runningServerOp);
        }
        else if (COMPOSITE.equals(operation.require(OP).asString())) {
            // Recurse into the steps to see what's required
            if (operation.hasDefined(STEPS)) {
                int stepNum = 0;
                List<ParsedOp> parsedSteps = new ArrayList<ParsedOp>();
                for (ModelNode step : operation.get(STEPS).asList()) {
                    parsedSteps.add(parseOperation(step, stepNum++));
                }
                result = new ParsedMultiStepOp(index, parsedSteps);
            }
            else {
                // Will fail later
                result = new SimpleParsedOp(index, operation, address);
            }
        }
        else {
            result = new SimpleParsedOp(index, operation, address);
        }

        return result;
    }

    private ModelNode createOverallResult(ModelNode opResult, ParsedOp parsedOp, DelegatingControllerTransactionContext tx) {
        if (!SUCCESS.equals(opResult.get(OUTCOME).asString())) {
            return opResult;
        }
        ModelNode resultNode = opResult.get(RESULT);

        ModelNode overallResult = new ModelNode();
        overallResult.get(OUTCOME).set(SUCCESS);
        ModelNode domainResult = parsedOp.getFormattedDomainResult(resultNode);
        overallResult.get(RESULT, DOMAIN_RESULTS).set(domainResult);
        ModelNode fullModel = tx.targetResource == null ? null : tx.targetResource.getUncommittedModel();
        if (fullModel == null) {
            fullModel = getDomainAndHostModel();
        }
        ModelNode hostModel = fullModel.get(HOST, localHostName);
        Map<Set<ServerIdentity>, ModelNode> serverOps = parsedOp.getServerOps(fullModel, hostModel);
        ModelNode serverOpsNode = overallResult.get(RESULT, SERVER_OPERATIONS);
        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : serverOps.entrySet()) {
            ModelNode setNode = serverOpsNode.add();
            ModelNode serverNode = setNode.get("servers");
            serverNode.setEmptyList();
            for (ServerIdentity server : entry.getKey()) {
                serverNode.add(server.getServerName(), server.getServerGroupName());
            }
            setNode.get(OP).set(entry.getValue());
        }
        ModelNode compOp = opResult.has(COMPENSATING_OPERATION) ? opResult.get(COMPENSATING_OPERATION) : null;
        if (compOp != null) {
            if (compOp.isDefined()) {
                compOp = parsedOp.getFormattedDomainCompensatingOp(compOp);
            }
            overallResult.get(COMPENSATING_OPERATION).set(compOp);
        }
        return overallResult;
    }

    private String getServerGroup(String serverName) {
        return getModel().require(HOST).require(localHostName).require(SERVER_CONFIG).require(serverName).require(GROUP).asString();
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerOperations(ModelNode domainOp, PathAddress domainOpAddress, ModelNode domainModel, ModelNode hostModel) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        OperationHandler handler = getRegistry().getOperationHandler(domainOpAddress, domainOp.require(OP).asString());
        if (!(handler instanceof ModelUpdateOperationHandler)) {
            result = Collections.emptyMap();
        }
        if (result == null) {
            result = serverOperationResolver.getServerOperations(domainOp, domainOpAddress, domainModel, hostModel);
        }
        return result;
    }

    private interface ParsedOp {
        ModelNode getDomainOperation();
        Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel);
        ModelNode getFormattedDomainResult(ModelNode resultNode);
        ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted);
    }

    private class SimpleParsedOp implements ParsedOp {
        private final String domainStep;
        private final ModelNode domainOp;
        private final PathAddress domainOpAddress;
        private Map<Set<ServerIdentity>, ModelNode> serverOps;

        private SimpleParsedOp(int index) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = null;
            this.domainOpAddress = null;
            this.serverOps = Collections.emptyMap();
        }

        private SimpleParsedOp(int index, ModelNode domainOp, final PathAddress domainOpAddress) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = domainOp;
            this.domainOpAddress = domainOpAddress;
            this.serverOps = null;
        }

        private SimpleParsedOp(int index, String serverName, ModelNode serverOp) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = null;
            this.domainOpAddress = null;
            final ServerIdentity serverIdentity = new ServerIdentity(localHostName, getServerGroup(serverName), serverName);
            this.serverOps = Collections.singletonMap(Collections.singleton(serverIdentity), serverOp);
        }

        @Override
        public Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel) {
            Map<Set<ServerIdentity>, ModelNode> result = serverOps;
            if (serverOps == null) {
                result = DomainModelImpl.this.getServerOperations(domainOp, domainOpAddress, domainModel, hostModel);
            }
            return result;
        }

        @Override
        public ModelNode getDomainOperation() {
            return domainOp;
        }

        @Override
        public ModelNode getFormattedDomainResult(ModelNode resultNode) {
            ModelNode formatted = new ModelNode();
            formatted.get(domainStep).set(resultNode);
            return formatted;
        }

        @Override
        public ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted) {
            return unformatted;
        }
    }

    private class ParsedMultiStepOp implements ParsedOp {

        private final String domainStep;
        private final List<ParsedOp> steps;

        private ParsedMultiStepOp(final int index, final List<ParsedOp> steps) {
            this.domainStep = "step-" + (index + 1);
            this.steps = steps;
        }

        @Override
        public Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel) {
            Map<Set<ServerIdentity>, List<ModelNode>> buildingBlocks = new HashMap<Set<ServerIdentity>, List<ModelNode>>();
            for (ParsedOp step : steps) {
                Map<Set<ServerIdentity>, ModelNode> stepResult = step.getServerOps(domainModel, hostModel);
                if (stepResult.size() == 0) {
                    continue;
                }
                else if (buildingBlocks.size() == 0) {
                    for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                        List<ModelNode> list = new ArrayList<ModelNode>();
                        list.add(entry.getValue());
                        buildingBlocks.put(entry.getKey(), list);
                    }
                }
                else {
                    for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                        List<ModelNode> existingOp = buildingBlocks.get(entry.getKey());
                        if (existingOp != null) {
                            existingOp.add(entry.getValue());
                        }
                        else {
                            Set<ServerIdentity> newSet = new HashSet<ServerIdentity>(entry.getKey());
                            Set<Set<ServerIdentity>> existingSets = new HashSet<Set<ServerIdentity>>(buildingBlocks.keySet());
                            for (Set<ServerIdentity> existing : existingSets) {
                                Set<ServerIdentity> copy = new HashSet<ServerIdentity>(existing);
                                copy.retainAll(newSet);
                                if (copy.size() > 0) {
                                    if (copy.size() == existing.size()) {
                                        // Just add the new step and store back
                                        buildingBlocks.get(existing).add(entry.getValue());
                                    }
                                    else {
                                        // Add the new step to the intersection; store the old set of steps
                                        // under a key that includes the remainder
                                        List<ModelNode> existingSteps = buildingBlocks.remove(existing);
                                        List<ModelNode> newSteps = new ArrayList<ModelNode>(existingSteps);
                                        buildingBlocks.put(copy, newSteps);
                                        existing.removeAll(copy);
                                        buildingBlocks.put(existing, existingSteps);
                                    }

                                    // no longer track the servers we've stored
                                    newSet.removeAll(copy);
                                }
                            }

                            // Any servers not stored above get their own entry
                            if (newSet.size() > 0) {
                                List<ModelNode> toAdd = new ArrayList<ModelNode>();
                                toAdd.add(entry.getValue());
                                buildingBlocks.put(newSet, toAdd);
                            }
                        }
                    }
                }
            }

            Map<Set<ServerIdentity>, ModelNode> result = null;
            if (buildingBlocks.size() > 0) {
                result = new HashMap<Set<ServerIdentity>, ModelNode>();
                for (Map.Entry<Set<ServerIdentity>, List<ModelNode>> entry : buildingBlocks.entrySet()) {
                    List<ModelNode> ops = entry.getValue();
                    if (ops.size() == 1) {
                        result.put(entry.getKey(), ops.get(0));
                    }
                    else {
                        ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                        ModelNode steps = composite.get(STEPS);
                        for (ModelNode step : entry.getValue()) {
                            steps.add(step);
                        }
                        result.put(entry.getKey(), composite);
                    }
                }
            }
            else {
                result = Collections.emptyMap();
            }
            return result;
        }

        @Override
        public ModelNode getDomainOperation() {
            ModelNode result = null;
            List<ModelNode> domainSteps = new ArrayList<ModelNode>();
            for (ParsedOp step : steps) {
                ModelNode stepNode = step.getDomainOperation();
                if (stepNode != null) {
                    domainSteps.add(stepNode);
                }
            }
            if (domainSteps.size() == 1) {
                result = domainSteps.get(0);
            }
            else if (domainSteps.size() > 1) {
                ModelNode stepsParam = new ModelNode();
                for (ModelNode stepNode : domainSteps) {
                    stepsParam.add(stepNode);
                }
                result = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                result.get(STEPS).set(stepsParam);
            }
            return result;
        }

        @Override
        public ModelNode getFormattedDomainResult(ModelNode resultNode) {
            ModelNode formatted = new ModelNode();
            int resultStep = 0;
            for (int i = 0; i < steps.size(); i++) {
                ParsedOp po = steps.get(i);
                if (po.getDomainOperation() != null) {
                    String label = "step-" + (++resultStep);
                    ModelNode stepResultNode = resultNode.get(label);
                    ModelNode formattedStepResultNode = po.getFormattedDomainResult(stepResultNode);
                    formatted.get("step-" + (i+1)).set(formattedStepResultNode);
                }
                else {
                    formatted.get("step-" + (i+1), OUTCOME).set(IGNORED);
                }
            }
            return formatted;
        }

        @Override
        public ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted) {
            ModelNode formatted = new ModelNode();
            int resultStep = 0;
            for (int i = 0; i < steps.size(); i++) {
                ParsedOp po = steps.get(i);
                if (po.getDomainOperation() != null) {
                    String label = "step-" + (++resultStep);
                    ModelNode stepResultNode = unformatted.get(label);
                    formatted.get("step-" + (i+1)).set(stepResultNode);
                }
                else {
                    formatted.get("step-" + (i+1)).set(IGNORED);
                }
            }
            return formatted;
        }
    }

    private static interface UncommittedModelProviderControllerResource extends ControllerResource {
        ModelNode getUncommittedModel();
    }

    private class DelegatingControllerTransactionContext implements ControllerTransactionContext {

        private final ControllerTransactionContext delegate;
        private UncommittedModelProviderControllerResource targetResource;

        private DelegatingControllerTransactionContext(final ControllerTransactionContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public ModelNode getTransactionId() {
            return delegate.getTransactionId();
        }

        @Override
        public void registerResource(ControllerResource resource) {
            delegate.registerResource(resource);
            if (resource instanceof UncommittedModelProviderControllerResource) {
                if (targetResource != null) {
                    throw new IllegalStateException("UncommittedModelProviderControllerResource already registered");
                }
                targetResource = (UncommittedModelProviderControllerResource) resource;
            }
        }

        @Override
        public void deregisterResource(ControllerResource resource) {
            delegate.deregisterResource(resource);
        }

        @Override
        public void setRollbackOnly() {
            delegate.setRollbackOnly();
        }

    }

    /**
     * Hack to pass both the domain and host configuration persisters through to
     * TransactionalMultiStepOperationController via the getMultiStepOperationController method.
     */
    private static class DualRootConfigurationPersisterProvider implements ConfigurationPersisterProvider {

        private final ConfigurationPersisterProvider domainProvider;
        private final ConfigurationPersisterProvider hostProvider;
        private final boolean forHost;

        DualRootConfigurationPersisterProvider(final ConfigurationPersisterProvider domainProvider,
                final ConfigurationPersisterProvider hostProvider, final boolean forHost) {
            this.domainProvider = domainProvider;
            this.hostProvider = hostProvider;
            this.forHost = forHost;
        }

        @Override
        public ConfigurationPersister getConfigurationPersister() {
            return forHost ? hostProvider.getConfigurationPersister() : domainProvider.getConfigurationPersister();
        }

    }

    protected class TransactionalMultiStepOperationController extends MultiStepOperationController {

        protected final ControllerTransactionContext transaction;
        protected boolean hostModelUpdated;
        protected final ConfigurationPersisterProvider hostPersisterProvider;
        /** Instead of persisting, this persister records that host model was modified and needs to be persisted when all steps are done. */
        protected final ConfigurationPersisterProvider localHostConfigPersisterProvider = new ConfigurationPersisterProvider() {
            @Override
            public org.jboss.as.controller.persistence.ConfigurationPersister getConfigurationPersister() {
                return new ConfigurationPersister() {
                    @Override
                    public void store(ModelNode model) throws ConfigurationPersistenceException {
                        hostModelUpdated = true;
                    }

                    @Override
                    public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
                        // an UnsupportedOperationException is also fine if this delegation needs to be removed
                        // in some refactor someday
                        DomainModelImpl.this.injectedHostPersister.marshallAsXml(model, output);
                    }

                    @Override
                    public List<ModelNode> load() throws ConfigurationPersistenceException {
                        throw new UnsupportedOperationException("load() should not be called as part of operation handling");
                    }

                    @Override
                    public void successfulBoot() throws ConfigurationPersistenceException {
                    }

                    @Override
                    public String snapshot() {
                        return null;
                    }

                    @Override
                    public SnapshotInfo listSnapshots() {
                        return NULL_SNAPSHOT_INFO;
                    }

                    @Override
                    public void deleteSnapshot(String name) {
                    }
                };
            }
        };

        protected TransactionalMultiStepOperationController(final Operation operation, final ResultHandler resultHandler,
                final ModelProvider modelSource, final DualRootConfigurationPersisterProvider persisterProvider,
                final ControllerTransactionContext transaction) throws OperationFailedException {

            super(operation, resultHandler, modelSource, persisterProvider.domainProvider);
            this.transaction = transaction;
            this.hostPersisterProvider = persisterProvider.hostProvider;
        }

        @Override
        protected OperationResult executeStep(final ModelNode step, final ResultHandler stepResultHandler) {
            boolean forHost = isOperationForHost(operation.getOperation());
            return DomainModelImpl.this.execute(operation.clone(step), stepResultHandler, getOperationControllerContext(forHost), resolve);
        }

        private OperationControllerContext getOperationControllerContext(boolean forHost) {
            final ConfigurationPersisterProvider persisterProvider = new DualRootConfigurationPersisterProvider(this, localHostConfigPersisterProvider, forHost);
            return new OperationControllerContext() {

                @Override
                public OperationContextFactory getOperationContextFactory() {
                    return TransactionalMultiStepOperationController.this;
                }

                @Override
                public ModelProvider getModelProvider() {
                    return TransactionalMultiStepOperationController.this;
                }

                @Override
                public ControllerTransactionContext getControllerTransactionContext() {
                    return null;
                }

                @Override
                public ConfigurationPersisterProvider getConfigurationPersisterProvider() {
                    return persisterProvider;
                }
            };
        }

        @Override
        protected boolean isModelUpdated() {
            return modelUpdated || hostModelUpdated;
        }

        /** Instead of updating and persisting, we register a resource that does it at commit */
        @Override
        protected void updateModelAndPersist() {
            if (transaction == null) {
                TransactionalMultiStepOperationController.this.commit();
            }
            else {
                ControllerResource resource = new UncommittedModelProviderControllerResource() {

                    @Override
                    public void commit() {
                        TransactionalMultiStepOperationController.this.commit();
                    }

                    @Override
                    public void rollback() {
                        // no-op
                    }

                    @Override
                    public ModelNode getUncommittedModel() {
                        return localModel;
                    }

                };
                transaction.registerResource(resource);
            }
        }

        private void commit() {

            final ModelNode model = modelSource.getModel();
            synchronized (model) {
                model.set(localModel);
            }
            if (modelUpdated) {
                DomainModelImpl.this.persistConfiguration(model, injectedConfigPersisterProvider);
            }
            if (hostModelUpdated) {
                DomainModelImpl.this.persistConfiguration(model, hostPersisterProvider);
            }
        }

    }

    private class DomainModelControllerResource implements UncommittedModelProviderControllerResource {

        private final PathAddress address;
        private final ModelNode subModel;
        private final boolean isRemove;
        private final OperationControllerContext operationControllerContext;

        public DomainModelControllerResource(final OperationHandler handler, final PathAddress address, final ModelNode subModel,
                final OperationControllerContext operationControllerContext) {
            if (handler instanceof ModelUpdateOperationHandler) {
                this.address = address;
                this.subModel = subModel;
                this.isRemove = (handler instanceof ModelRemoveOperationHandler);
                this.operationControllerContext = operationControllerContext;
            }
            else {
                this.address = null;
                this.subModel = null;
                this.isRemove = false;
                this.operationControllerContext = null;
            }
        }

        @Override
        public void commit() {
            if (address != null) {
                final ModelNode model = operationControllerContext.getModelProvider().getModel();
                synchronized (model) {
                    if (isRemove) {
                        address.remove(model);
                    } else {
                        address.navigate(model, true).set(subModel);
                    }
                    persistConfiguration(model, operationControllerContext.getConfigurationPersisterProvider());
                }
            }
        }

        @Override
        public void rollback() {
            // no-op
        }

        @Override
        public ModelNode getUncommittedModel() {
            ModelNode model = null;
            if (address != null) {
                model = operationControllerContext.getModelProvider().getModel();
                synchronized (model) {
                    model = model.clone();
                }
                if (isRemove) {
                    address.remove(model);
                } else {
                    address.navigate(model, true).set(subModel);
                }
            }
            return model;
        }
    }
}
