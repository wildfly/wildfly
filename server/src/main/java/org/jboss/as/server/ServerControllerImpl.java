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

package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import java.util.EnumMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelProvider;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContextImpl;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentReplaceHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.server.deployment.DeploymentUploadURLHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.ExtensionRemoveHandler;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.NativeManagementAddHandler;
import org.jboss.as.server.operations.ServerOperationHandlers;
import org.jboss.as.server.operations.ServerReloadHandler;
import org.jboss.as.server.operations.ServerSocketBindingAddHandler;
import org.jboss.as.server.operations.ServerSocketBindingRemoveHandler;
import org.jboss.as.server.operations.SocketBindingGroupAddHandler;
import org.jboss.as.server.operations.SocketBindingGroupRemoveHandler;
import org.jboss.as.server.operations.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.operations.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ServerControllerImpl extends BasicModelController implements ServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ExecutorService executorService;
    private final ServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final ServerEnvironment serverEnvironment;
    private final AtomicInteger stamp = new AtomicInteger(0);
    private final AtomicStampedReference<State> state = new AtomicStampedReference<State>(null, 0);
    private final ExtensibleConfigurationPersister extensibleConfigurationPersister;
    private final DeploymentRepository deploymentRepository;
    private final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);

    ServerControllerImpl(final ServiceContainer container, final ServiceTarget serviceTarget, final ServerEnvironment serverEnvironment,
            final ExtensibleConfigurationPersister configurationPersister, final DeploymentRepository deploymentRepository,
            final ExecutorService executorService) {
        super(createCoreModel(), configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
        this.serviceTarget = serviceTarget;
        this.extensibleConfigurationPersister = configurationPersister;
        this.serverEnvironment = serverEnvironment;
        this.deploymentRepository = deploymentRepository;
        this.serviceRegistry = new DelegatingServiceRegistry(container);
        this.executorService = executorService;
    }

    void init() {
        state.set(State.STARTING, stamp.incrementAndGet());

        registerInternalOperations();

        // Build up the core model registry
        ModelNodeRegistration root = getRegistry();
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);
        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, ServerOperationHandlers.SERVER_READ_RESOURCE_HANDLER, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, ServerOperationHandlers.SERVER_READ_ATTRIBUTE_HANDLER, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, ServerOperationHandlers.SERVER_WRITE_ATTRIBUTE_HANDLER, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE, SystemPropertyAddHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        DeploymentUploadBytesHandler dubh = new DeploymentUploadBytesHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentUploadBytesHandler.OPERATION_NAME, dubh, dubh, false);
        DeploymentUploadURLHandler duuh = new DeploymentUploadURLHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentUploadURLHandler.OPERATION_NAME, duuh, duuh, false);
        root.registerOperationHandler(DeploymentReplaceHandler.OPERATION_NAME, DeploymentReplaceHandler.INSTANCE, DeploymentReplaceHandler.INSTANCE, false);
        DeploymentFullReplaceHandler dfrh = new DeploymentFullReplaceHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentFullReplaceHandler.OPERATION_NAME, dfrh, dfrh, false);
//        root.registerOperationHandler(ServerCompositeOperationHandler.OPERATION_NAME, ServerCompositeOperationHandler.INSTANCE, ServerCompositeOperationHandler.INSTANCE, false);

        // Runtime operations
        root.registerOperationHandler(ServerReloadHandler.OPERATION_NAME, ServerReloadHandler.INSTANCE, ServerReloadHandler.INSTANCE, false);

        // Management API protocols
        ModelNodeRegistration managementNative = root.registerSubModel(PathElement.pathElement(MANAGEMENT, NATIVE_API), CommonProviders.MANAGEMENT_PROVIDER);
        managementNative.registerOperationHandler(NativeManagementAddHandler.OPERATION_NAME, NativeManagementAddHandler.INSTANCE, NativeManagementAddHandler.INSTANCE, false);

        ModelNodeRegistration managementHttp = root.registerSubModel(PathElement.pathElement(MANAGEMENT, HTTP_API), CommonProviders.MANAGEMENT_PROVIDER);
        managementHttp.registerOperationHandler(HttpManagementAddHandler.OPERATION_NAME, HttpManagementAddHandler.INSTANCE, HttpManagementAddHandler.INSTANCE, false);
        // root.registerReadWriteAttribute(ModelDescriptionConstants.MANAGEMENT, GlobalOperationHandlers.READ_ATTRIBUTE, ManagementSocketAddHandler.INSTANCE);

        // Paths
        ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(SpecifiedPathAddHandler.OPERATION_NAME, SpecifiedPathAddHandler.INSTANCE, SpecifiedPathAddHandler.INSTANCE, false);
        paths.registerOperationHandler(SpecifiedPathRemoveHandler.OPERATION_NAME, SpecifiedPathRemoveHandler.INSTANCE, SpecifiedPathRemoveHandler.INSTANCE, false);

        // Interfaces
        ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(SpecifiedInterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(SpecifiedInterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);

        // Sockets
        ModelNodeRegistration socketGroup = root.registerSubModel(PathElement.pathElement(SOCKET_BINDING_GROUP), ServerDescriptionProviders.SOCKET_BINDING_GROUP_PROVIDER);
        socketGroup.registerOperationHandler(SocketBindingGroupAddHandler.OPERATION_NAME, SocketBindingGroupAddHandler.INSTANCE, SocketBindingGroupAddHandler.INSTANCE, false);
        socketGroup.registerOperationHandler(SocketBindingGroupRemoveHandler.OPERATION_NAME, SocketBindingGroupRemoveHandler.INSTANCE, SocketBindingGroupRemoveHandler.INSTANCE, false);
        ModelNodeRegistration socketBinding = socketGroup.registerSubModel(PathElement.pathElement(SOCKET_BINDING), CommonProviders.SOCKET_BINDING_PROVIDER);
        socketBinding.registerOperationHandler(ServerSocketBindingAddHandler.OPERATION_NAME, ServerSocketBindingAddHandler.INSTANCE, ServerSocketBindingAddHandler.INSTANCE, false);
        socketBinding.registerOperationHandler(ServerSocketBindingRemoveHandler.OPERATION_NAME, ServerSocketBindingRemoveHandler.INSTANCE, ServerSocketBindingRemoveHandler.INSTANCE, false);

        // Deployments
        ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);
        DeploymentAddHandler dah = new DeploymentAddHandler(deploymentRepository);
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah, false);
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, DeploymentRemoveHandler.INSTANCE, DeploymentRemoveHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentDeployHandler.OPERATION_NAME, DeploymentDeployHandler.INSTANCE, DeploymentDeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentUndeployHandler.OPERATION_NAME, DeploymentUndeployHandler.INSTANCE, DeploymentUndeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentRedeployHandler.OPERATION_NAME, DeploymentRedeployHandler.INSTANCE, DeploymentRedeployHandler.INSTANCE, false);

        // Extensions
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(getRegistry(), deployments, extensibleConfigurationPersister);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        deployers.clear();
        for (Phase phase : Phase.values()) {
            deployers.put(phase, new ConcurrentSkipListSet<RegisteredProcessor>());
        }

    }

    private static ModelNode createCoreModel() {
        ModelNode root = new ModelNode();
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(NAME);
        root.get(MANAGEMENT);
        root.get(PROFILE_NAME);
        root.get(SYSTEM_PROPERTIES);
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SUBSYSTEM);
        root.get(INTERFACE);
        root.get(SOCKET_BINDING_GROUP);
        root.get(DEPLOYMENT);
        return root;
    }

    EnumMap<Phase, SortedSet<RegisteredProcessor>> finishBoot() {
        state.set(State.RUNNING, stamp.incrementAndGet());
        EnumMap<Phase, SortedSet<RegisteredProcessor>> copy = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);
        for (Map.Entry<Phase, SortedSet<RegisteredProcessor>> entry : deployers.entrySet()) {
            copy.put(entry.getKey(), new ConcurrentSkipListSet<RegisteredProcessor>(entry.getValue()));
        }
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public ServerEnvironment getServerEnvironment() {
        return serverEnvironment;
    }

    /**
     * Get this server's service container registry.
     *
     * @return the container registry
     */
    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Get the server controller state.
     *
     * @return the state
     */
    @Override
    public State getState() {
        return state.getReference();
    }

    /** {@inheritDoc} */
    @Override
    protected OperationContext getOperationContext(final ModelNode subModel, final OperationHandler operationHandler) {
        if (operationHandler instanceof BootOperationHandler) {
            if (getState() == State.STARTING) {
                return new BootContextImpl(subModel, getRegistry(), deployers);
            } else {
                state.set(State.RESTART_REQUIRED, stamp.incrementAndGet());
                return super.getOperationContext(subModel, operationHandler);
            }
        } else if (!(getState() == State.RESTART_REQUIRED && operationHandler instanceof ModelUpdateOperationHandler)) {
            return new ServerOperationContextImpl(this, getRegistry(), subModel);
        } else {
            return super.getOperationContext(subModel, operationHandler);
        }
    }

    @Override
    protected OperationResult doExecute(OperationContext context, ModelNode operation, OperationHandler operationHandler, ResultHandler resultHandler, PathAddress address, ModelProvider modelProvider, ConfigurationPersisterProvider configurationPersisterFactory) throws OperationFailedException {
        boolean rollback = isRollbackOnRuntimeFailure(context, operation);
        RollbackAwareResultHandler rollbackAwareHandler = null;
        if (rollback) {
            rollbackAwareHandler = new RollbackAwareResultHandler(resultHandler);
            resultHandler = rollbackAwareHandler;
        }
        final OperationResult result = super.doExecute(context, operation, operationHandler, resultHandler, address, modelProvider, configurationPersisterFactory);
        if(context instanceof ServerOperationContextImpl) {
            if (rollbackAwareHandler != null) {
                rollbackAwareHandler.setRollbackOperation(result.getCompensatingOperation());
                // TODO deal with Cancellable as well
            }
            final ServerOperationContextImpl serverOperationContext = ServerOperationContextImpl.class.cast(context);
            if(serverOperationContext.getRuntimeTask() != null) {
                try {
                    serverOperationContext.getRuntimeTask().execute(new RuntimeTaskContext() {
                        @Override
                        public ServiceTarget getServiceTarget() {
                            return serviceTarget;
                        }

                        @Override
                        public ServiceRegistry getServiceRegistry() {
                            return serviceRegistry;
                        }
                    });
                } catch (OperationFailedException e) {
                    resultHandler.handleFailed(e.getFailureDescription());
                } catch (Exception e) {
                    resultHandler.handleFailed(new ModelNode().set(e.toString()));
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void persistConfiguration(final ModelNode model, final ConfigurationPersisterProvider configurationPersisterFactory) {
        // do not persist during startup
        if (getState() != State.STARTING) {
            super.persistConfiguration(model, configurationPersisterFactory);
        }
    }

    @Override
    protected MultiStepOperationController getMultiStepOperationController(ModelNode operation, ResultHandler handler,
            ModelProvider modelSource) throws OperationFailedException {
        return new ServerMultiStepOperationController(operation, handler, modelSource);
    }

    private boolean isRollbackOnRuntimeFailure(OperationContext context, ModelNode operation) {
        return context instanceof ServerOperationContextImpl &&
            (!operation.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || operation.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean());
    }

    private class ServerOperationContextImpl extends OperationContextImpl implements ServerOperationContext, RuntimeOperationContext {
        // -1 as initial value ensures the CAS in revertRestartRequired()
        // will never succeed unless restartRequired() is called
        private int ourStamp = -1;
        private RuntimeTask runtimeTask;

        public ServerOperationContextImpl(ModelController controller, ModelNodeRegistration registry, ModelNode subModel) {
            super(controller, registry, subModel);
        }

        @Override
        public ServerController getController() {
            return (ServerController) super.getController();
        }

        @Override
        public synchronized void restartRequired() {
            AtomicStampedReference<State> stateRef = ServerControllerImpl.this.state;
            int newStamp = ServerControllerImpl.this.stamp.incrementAndGet();
            int[] receiver = new int[1];
            // Keep trying until stateRef is RESTART_REQUIRED with our stamp
            for (;;) {
                State was = stateRef.get(receiver);
                if (was == State.STARTING) {
                    break;
                }
                if (stateRef.compareAndSet(was, State.RESTART_REQUIRED, receiver[0], newStamp)) {
                    ourStamp = newStamp;
                    break;
                }
            }
        }

        @Override
        public synchronized void revertRestartRequired() {
            // If 'state' still has the state we last set in restartRequired(), change to RUNNING
            ServerControllerImpl.this.state.compareAndSet(State.RESTART_REQUIRED, State.RUNNING,
                    ourStamp, ServerControllerImpl.this.stamp.incrementAndGet());
        }

        @Override
        public RuntimeOperationContext getRuntimeContext() {
            return this;
        }

        public RuntimeTask getRuntimeTask() {
            return runtimeTask;
        }

        @Override
        public void setRuntimeTask(RuntimeTask runtimeTask) {
            this.runtimeTask = runtimeTask;
        }
    }

    private class BootContextImpl extends ServerOperationContextImpl implements BootOperationContext {

        private final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers;

        private BootContextImpl(final ModelNode subModel, final ModelNodeRegistration registry, final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers) {
            super(ServerControllerImpl.this, registry, subModel);
            this.deployers = deployers;
        }

        @Override
        public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
            if (phase == null) {
                throw new IllegalArgumentException("phase is null");
            }
            if (processor == null) {
                throw new IllegalArgumentException("processor is null");
            }
            if (priority < 0) {
                throw new IllegalArgumentException("priority is invalid (must be >= 0)");
            }
            deployers.get(phase).add(new RegisteredProcessor(priority, processor));
        }
    }

    static final class RegisteredProcessor implements Comparable<RegisteredProcessor> {
        private final int priority;
        private final DeploymentUnitProcessor processor;

        RegisteredProcessor(final int priority, final DeploymentUnitProcessor processor) {
            this.priority = priority;
            this.processor = processor;
        }

        @Override
        public int compareTo(final RegisteredProcessor o) {
            final int rel = Integer.signum(priority - o.priority);
            return rel == 0 ? processor.getClass().getName().compareTo(o.getClass().getName()) : rel;
        }

        int getPriority() {
            return priority;
        }

        DeploymentUnitProcessor getProcessor() {
            return processor;
        }

    }

    class RollbackAwareResultHandler implements ResultHandler {

        private final ResultHandler delegate;
        private ModelNode rollbackOperation;

        public RollbackAwareResultHandler(ResultHandler resultHandler) {
            this.delegate = resultHandler;
        }

        @Override
        public void handleResultFragment(String[] location, ModelNode result) {
            delegate.handleResultFragment(location, result);
        }

        @Override
        public void handleResultComplete() {
            delegate.handleResultComplete();
        }

        @Override
        public void handleFailed(final ModelNode failureDescription) {

            if (rollbackOperation == null) {
                delegate.handleFailed(failureDescription);
                return;
            }

            final ResultHandler rollbackHandler = new ResultHandler() {

                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                    // ignore fragments from rollback
                }

                @Override
                public void handleResultComplete() {
                    // FIXME this will not appear in the correct location
//                    ModelNode rollbackResult = new ModelNode();
//                    rollbackResult.get("rolled-back").set(true);
//                    delegate.handleResultFragment(new String[0], rollbackResult);
                    delegate.handleFailed(failureDescription);
                }

                @Override
                public void handleFailed(ModelNode rollbackFailureDescription) {
                    // FIXME this will not appear in the correct location
                    ModelNode rollbackResult = new ModelNode();
//                    rollbackResult.get("rolled-back").set(false);
//                    delegate.handleResultFragment(new String[0], rollbackResult);
                    rollbackResult = new ModelNode();
                    rollbackResult.get("rollback-failure-description").set(rollbackFailureDescription);
                    delegate.handleFailed(failureDescription);
                }

                @Override
                public void handleCancellation() {
                    handleFailed(new ModelNode().set("Rollback was cancelled"));
                }
            };

            // Make sure the rollback op doesn't itself try and roll back
            rollbackOperation.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    execute(rollbackOperation, rollbackHandler);
                }

            };
            executorService.execute(r);
        }

        @Override
        public void handleCancellation() {
            delegate.handleCancellation();
        }

        private void setRollbackOperation(ModelNode compensatingOperation) {
            this.rollbackOperation = compensatingOperation;
        }
    }

    private class ServerMultiStepOperationController extends MultiStepOperationController {

        private final boolean rollbackOnRuntimeFailure;

        private ServerMultiStepOperationController(final ModelNode operation, final ResultHandler resultHandler,
                final ModelProvider modelSource) throws OperationFailedException {
            super(operation, resultHandler, modelSource);
            this.rollbackOnRuntimeFailure = (!operation.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || operation.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean());
        }

        @Override
        public OperationContext getOperationContext(ModelProvider modelSource, PathAddress address,
                OperationHandler operationHandler) {
            OperationContext delegate = super.getOperationContext(modelSource, address, operationHandler);
            return delegate.getRuntimeContext() == null ? delegate : new StepRuntimeOperationContext(Integer.valueOf(currentOperation), ServerOperationContext.class.cast(delegate));
        }

        @Override
        protected void handleFailures() {
            if (!rollbackOnRuntimeFailure || !modelComplete.get()) {
                super.handleFailures();
            }
            else {
                final ModelNode compensatingOp = getOverallCompensatingOperation();
                final ResultHandler rollbackResultHandler = new RollbackResultHandler();
                // Execute the rollback in another thread as this method may be called by an MSC thread
                // and we don't want to risk blocking it
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        ServerControllerImpl.this.execute(compensatingOp, rollbackResultHandler);
                    }
                };
                ServerControllerImpl.this.executorService.execute(r);
            }
        }

        @Override
        protected void recordModelComplete() {
            super.recordModelComplete();

            if (runtimeTasks.size() > 0) {
                RuntimeTaskContext rtc = new RuntimeTaskContext() {
                    @Override
                    public ServiceTarget getServiceTarget() {
                        return serviceTarget;
                    }

                    @Override
                    public ServiceRegistry getServiceRegistry() {
                        return serviceRegistry;
                    }
                };
                for(int i = 0; i < steps.size(); i++) {
                    Integer id = Integer.valueOf(i);
                    RuntimeTask runtimeTask = runtimeTasks.get(id);
                    if (runtimeTask == null) {
                        continue;
                    }
                    try {
                        runtimeTask.execute(rtc);
                    } catch (OperationFailedException e) {
                        stepResultHandlers.get(id).handleFailed(e.getFailureDescription());
                    } catch (Throwable t) {
                        stepResultHandlers.get(id).handleFailed(new ModelNode().set(t.toString()));
                    }
                }
            }
        }

        private void rollbackComplete(final ModelNode rollbackResult) {

            // Update each of our steps to indicate what happened with the rollback
            synchronized (resultsNode) {
                for (int i = 0; i < steps.size(); i++) {
                    String stepKey = getStepKey(i);
                    ModelNode stepResult = resultsNode.get(stepKey);
                    if (stepResult.hasDefined(OUTCOME) && !CANCELLED.equals(stepResult.get(OUTCOME).asString())) {

                        ModelNode rollbackStepOutcome = null;
                        ModelNode rollbackStepResult = null;
                        String rollbackKey = rollbackStepNames.get(Integer.valueOf(i));
                        if (rollbackKey != null) {
                            rollbackStepResult = rollbackResult.get(rollbackKey);
                            rollbackStepOutcome = rollbackStepResult.isDefined() ? rollbackStepResult.get(OUTCOME) : null;
                        }

                        if (rollbackStepOutcome == null || !rollbackStepOutcome.isDefined()) {
                            stepResult.get(ROLLED_BACK).set(false);
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(new ModelNode().set("No compensating operations was available"));
                        }
                        else if (CANCELLED.equals(rollbackStepOutcome.asString())) {
                            stepResult.get(ROLLED_BACK).set(false);
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(new ModelNode().set("Execution of the compensating operation was cancelled"));
                        }
                        else if (SUCCESS.equals(rollbackStepOutcome.asString())) {
                            stepResult.get(ROLLED_BACK).set(true);
                        }
                        else {
                            stepResult.get(ROLLED_BACK).set(false);
                            ModelNode rollbackFailureCause = rollbackStepResult.get(FAILURE_DESCRIPTION);
                            if (!rollbackFailureCause.isDefined()) {
                                rollbackFailureCause = new ModelNode().set("Compensating operation was reverted due to failure of other compensating operations");
                            }
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(rollbackFailureCause);
                        }
                    }
                }
            }

            // Finally, notify the end user's result handler of completion
            super.handleFailures();
        }

        /** Context that stores any registered RuntimeTask under the step's id */
        private class StepRuntimeOperationContext implements ServerOperationContext, RuntimeOperationContext {

            private final Integer id;
            private final ServerOperationContext delegate;

            private StepRuntimeOperationContext(final Integer id, final ServerOperationContext delegate) {
                this.id = id;
                this.delegate = delegate;
            }

            @Override
            public ModelNode getSubModel() throws IllegalArgumentException {
                return delegate.getSubModel();
            }

            @Override
            public ModelNodeRegistration getRegistry() {
                return delegate.getRegistry();
            }

            @Override
            public ServerController getController() {
                return delegate.getController();
            }

            @Override
            public void restartRequired() {
                delegate.restartRequired();
            }

            @Override
            public void revertRestartRequired() {
                delegate.revertRestartRequired();
            }

            @Override
            public RuntimeOperationContext getRuntimeContext() {
                return this;
            }

            @Override
            public void setRuntimeTask(RuntimeTask runtimeTask) {
                runtimeTasks.put(id, runtimeTask);
            }
        }

        /**
         * Captures the result of executing compensating operations and then triggers
         * the final completion of the original operation.
         */
        private class RollbackResultHandler implements ResultHandler {

            private final ModelNode rollbackResult = new ModelNode();
            @Override
            public void handleResultFragment(String[] location, ModelNode result) {
                rollbackResult.get(location).set(result);
            }

            @Override
            public void handleResultComplete() {
                // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

            @Override
            public void handleFailed(ModelNode failureDescription) {
             // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

            @Override
            public void handleCancellation() {
             // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

        }

    }
}
