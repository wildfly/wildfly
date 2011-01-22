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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

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
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.ExtensionRemoveHandler;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewServerControllerImpl extends BasicModelController implements NewServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ServiceContainer container;
    private final ServiceRegistry serviceRegistry;
    private final ServerEnvironment serverEnvironment;
    private final AtomicInteger stamp = new AtomicInteger(0);
    private final AtomicStampedReference<State> state = new AtomicStampedReference<State>(null, 0);

    NewServerControllerImpl(final ServiceContainer container, final ServerEnvironment serverEnvironment, final NewConfigurationPersister configurationPersister) {
        super(configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
        this.container = container;
        this.serverEnvironment = serverEnvironment;
        serviceRegistry = new DelegatingServiceRegistry(container);
    }

    void init() {
        state.set(State.STARTING, stamp.incrementAndGet());

        createCoreModel();
        // Build up the core model registry
        ModelNodeRegistration root = getRegistry();
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1));
        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(SpecifiedPathAddHandler.OPERATION_NAME, SpecifiedPathAddHandler.INSTANCE, SpecifiedPathAddHandler.INSTANCE, false);
        paths.registerOperationHandler(SpecifiedPathRemoveHandler.OPERATION_NAME, SpecifiedPathRemoveHandler.INSTANCE, SpecifiedPathRemoveHandler.INSTANCE, false);

        ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        NewExtensionContext extensionContext = new NewExtensionContextImpl(getRegistry(), deployments);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

    }

    private void createCoreModel() {
        ModelNode root = getModel();
        root.get(NAMESPACE).setEmptyList();
        root.get(SCHEMA_LOCATION).setEmptyList();
        root.get(NAME);
        root.get(MANAGEMENT);
        root.get(PROFILE_NAME);
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SUBSYSTEM);
        root.get(INTERFACE);
        root.get(SOCKET_BINDING_GROUP);
        root.get(SYSTEM_PROPERTY);
        root.get(DEPLOYMENT);
    }

    void finishBoot() {
        state.set(State.RUNNING, stamp.incrementAndGet());
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
                return new BootContextImpl(subModel, getRegistry());
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
        public NewServerController getController() {
            return (NewServerController) super.getController();
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
            AtomicStampedReference<State> stateRef = NewServerControllerImpl.this.state;
            int newStamp = NewServerControllerImpl.this.stamp.incrementAndGet();
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
            NewServerControllerImpl.this.state.compareAndSet(State.RESTART_REQUIRED, State.RUNNING,
                        ourStamp, NewServerControllerImpl.this.stamp.incrementAndGet());
        }

    }

    private class BootContextImpl extends ServerOperationContextImpl implements NewBootOperationContext {

        private BootContextImpl(final ModelNode subModel, final ModelNodeRegistration registry) {
            super(NewServerControllerImpl.this, registry, subModel);
        }

        @Override
        public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
        }
    }

    private class RuntimeContextImpl extends ServerOperationContextImpl implements NewRuntimeOperationContext {

        private RuntimeContextImpl(final ModelNode subModel, final ModelNodeRegistration registry) {
            super(NewServerControllerImpl.this, registry, subModel);
        }
    }
}
