/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ReadTransformedResourceOperation;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * Internal class used by test framework.
 * Boots up the model controller used for the test
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ModelTestModelControllerService extends AbstractControllerService {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final StringConfigurationPersister persister;
    protected final TransformerRegistry transformerRegistry;
    private final boolean validateOps;
    private final RunningModeControl runningModeControl;
    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Exception error;
    private volatile boolean bootSuccess;

    protected ModelTestModelControllerService(final ProcessType processType, final RunningModeControl runningModeControl, final TransformerRegistry transformerRegistry,
                           final StringConfigurationPersister persister, boolean validateOps, final DescriptionProvider rootDescriptionProvider, ControlledProcessState processState) {
        super(processType, runningModeControl, persister,
                processState == null ? new ControlledProcessState(true) : processState, rootDescriptionProvider, null, ExpressionResolver.DEFAULT);
        this.persister = persister;
        this.transformerRegistry = transformerRegistry;
        this.validateOps = validateOps;
        this.runningModeControl = runningModeControl;
    }

    protected ModelTestModelControllerService(final ProcessType processType, final RunningModeControl runningModeControl, final TransformerRegistry transformerRegistry,
            final StringConfigurationPersister persister, final boolean validateOps, final DelegatingResourceDefinition rootResourceDefinition, ControlledProcessState processState) {
        super(processType, runningModeControl, persister,
                processState == null ? new ControlledProcessState(true) : processState, rootResourceDefinition, null,
                ExpressionResolver.DEFAULT);
        this.persister = persister;
        this.transformerRegistry = transformerRegistry;
        this.validateOps = validateOps;
        this.runningModeControl = runningModeControl;
    }

    public boolean isSuccessfulBoot() {
        return bootSuccess;
    }

    public Throwable getBootError() {
        return error;
    }

    RunningMode getRunningMode() {
        return runningModeControl.getRunningMode();
    }

    ProcessType getProcessType() {
        return processType;
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        this.rootRegistration = rootRegistration;
        initCoreModel(rootResource, rootRegistration);
        initExtraModel(rootResource, rootRegistration);
    }

    protected void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (transformerRegistry != null) {
            rootRegistration.registerOperationHandler(READ_TRANSFORMED_RESOURCE_OPERATION, new ReadTransformedResourceOperation(transformerRegistry), ReadTransformedResourceOperation.DESCRIPTION, true);
        }
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, ProcessType.STANDALONE_SERVER);

        rootRegistration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);
    }

    protected void initExtraModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
    }

    @Override
    protected boolean boot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) throws ConfigurationPersistenceException {
        try {
            preBoot(bootOperations, rollbackOnRuntimeFailure);
            if (validateOps) {
                new OperationValidator(rootRegistration).validateOperations(bootOperations);
            }
            bootSuccess = super.boot(persister.getBootOperations(), rollbackOnRuntimeFailure);
            return bootSuccess;
        } catch (Exception e) {
            error = e;
        } catch (Throwable t) {
            error = new Exception(t);
        } finally {
            postBoot();
        }
        return false;
    }

    protected void preBoot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) {
    }

    protected void postBoot() {
    }

    @Override
    protected void bootThreadDone() {
        try {
            super.bootThreadDone();
        } catch (Exception ignore) {
            //7.1.2 does not have this method
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            super.start(context);
        } catch (StartException e) {
            error = e;
            latch.countDown();
            throw e;
        } catch (Exception e) {
            error = e;
            latch.countDown();
            throw new StartException(e);
        }
    }

    public void waitForSetup() throws Exception {
        latch.await();
        if (error != null) {
            throw error;
        }
    }

    public ManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    /**
     * Grabs the current root resource
     *
     * @param kernelServices the kernel services used to access the controller
     */
    public static Resource grabRootResource(ModelTestKernelServices<?> kernelServices) {
        final AtomicReference<Resource> resourceRef = new AtomicReference<Resource>();
        ((ModelTestKernelServicesImpl<?>)kernelServices).internalExecute(new ModelNode(), new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                resourceRef.set(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
                context.getResult().setEmptyObject();
                context.stepCompleted();
            }
        });
        Resource rootResource = resourceRef.get();
        Assert.assertNotNull(rootResource);
        return rootResource;
    }

    @Override
    protected ModelNode internalExecute(ModelNode operation, OperationMessageHandler handler,
            OperationTransactionControl control, OperationAttachments attachments, OperationStepHandler prepareStep) {
        return super.internalExecute(operation, handler, control, attachments, prepareStep);
    }

    public  ModelNode internalExecute(ModelNode operation, OperationStepHandler handler) {
        return internalExecute(operation, OperationMessageHandler.DISCARD, OperationTransactionControl.COMMIT, null, handler);
    }

    public static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode model = new ModelNode();
            model.get(DESCRIPTION).set("The test model controller");
            return model;
        }
    };

    public static class DelegatingResourceDefinition implements ResourceDefinition {
        private volatile ResourceDefinition delegate;

        public void setDelegate(ResourceDefinition delegate) {
            this.delegate = delegate;
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            delegate.registerOperations(resourceRegistration);
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            delegate.registerChildren(resourceRegistration);
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            delegate.registerAttributes(resourceRegistration);
        }

        @Override
        public PathElement getPathElement() {
            return delegate.getPathElement();
        }

        @Override
        public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
            return delegate.getDescriptionProvider(resourceRegistration);
        }
    };

}
