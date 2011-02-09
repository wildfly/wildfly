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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import java.util.EnumMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.NewExtensionContextImpl;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewOperationContextImpl;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
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
import org.jboss.as.server.operations.ServerCompositeOperationHandler;
import org.jboss.as.server.operations.ServerOperationHandlers;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.NativeManagementAddHandler;
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
final class ServerControllerImpl extends BasicModelController implements ServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ServiceContainer container;
    private final ServiceRegistry serviceRegistry;
    private final ServerEnvironment serverEnvironment;
    private final AtomicInteger stamp = new AtomicInteger(0);
    private final AtomicStampedReference<State> state = new AtomicStampedReference<State>(null, 0);
    private final ExtensibleConfigurationPersister extensibleConfigurationPersister;
    private final DeploymentRepository deploymentRepository;
    private final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);

    ServerControllerImpl(final ServiceContainer container, final ServerEnvironment serverEnvironment,
            final ExtensibleConfigurationPersister configurationPersister, final DeploymentRepository deploymentRepository) {
        super(createCoreModel(), configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
        this.extensibleConfigurationPersister = configurationPersister;
        this.container = container;
        this.serverEnvironment = serverEnvironment;
        this.deploymentRepository = deploymentRepository;
        serviceRegistry = new DelegatingServiceRegistry(container);
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
        root.registerOperationHandler(ServerCompositeOperationHandler.OPERATION_NAME, ServerCompositeOperationHandler.INSTANCE, ServerCompositeOperationHandler.INSTANCE, false);

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
        NewExtensionContext extensionContext = new NewExtensionContextImpl(getRegistry(), deployments, extensibleConfigurationPersister);
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
    protected NewOperationContext getOperationContext(final ModelNode subModel, final ModelNode operation, final OperationHandler operationHandler) {
        if (operationHandler instanceof BootOperationHandler) {
            if (getState() == State.STARTING) {
                return new BootContextImpl(subModel, getRegistry(), deployers);
            } else {
                state.set(State.RESTART_REQUIRED, stamp.incrementAndGet());
                return super.getOperationContext(subModel, operation, operationHandler);
            }
        } else if (operationHandler instanceof RuntimeOperationHandler && ! (getState() == State.RESTART_REQUIRED && operationHandler instanceof ModelUpdateOperationHandler)) {
            return new RuntimeContextImpl(subModel, getRegistry());
        } else {
            return super.getOperationContext(subModel, operation, operationHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Cancellable doExecute(final NewOperationContext context, final ModelNode operation, final OperationHandler operationHandler, final ResultHandler resultHandler) {
        if (context instanceof NewBootOperationContext) {
            return ((BootOperationHandler)operationHandler).execute(context, operation, resultHandler);
        } else if (context instanceof NewRuntimeOperationContext) {
            return ((RuntimeOperationHandler)operationHandler).execute(context, operation, resultHandler);
        } else {
            return super.doExecute(context, operation, operationHandler, resultHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void persistConfiguration(final ModelNode model) {
        // do not persist during startup
        if (getState() != State.STARTING) {
            super.persistConfiguration(model);
        }
    }

    private class ServerOperationContextImpl extends NewOperationContextImpl implements NewServerOperationContext {

        // -1 as initial value ensures the CAS in revertRestartRequired()
        // will never succeed unless restartRequired() is called
        private int ourStamp = -1;

        public ServerOperationContextImpl(ModelController controller, ModelNodeRegistration registry, ModelNode subModel) {
            super(controller, registry, subModel);
        }

        @Override
        public ServerController getController() {
            return (ServerController) super.getController();
        }

        @Override
        public ServiceTarget getServiceTarget() {
            // TODO: A tracking service listener which will somehow call complete when the operation is done
            return container;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
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

    }

    private class BootContextImpl extends ServerOperationContextImpl implements NewBootOperationContext {

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

    private class RuntimeContextImpl extends ServerOperationContextImpl implements NewRuntimeOperationContext {

        private RuntimeContextImpl(final ModelNode subModel, final ModelNodeRegistration registry) {
            super(ServerControllerImpl.this, registry, subModel);
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
}
