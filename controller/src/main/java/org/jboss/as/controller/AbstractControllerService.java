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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A base class for controller services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractControllerService implements Service<ModelController> {

    /**
     * Name of the system property to set to control the stack size for the thread used to process boot operations.
     * The boot sequence can have a very deep stack, so if needed setting this property can be used to create a larger
     * memory area for storing data on the stack.
     *
     * @see #DEFAULT_BOOT_STACK_SIZE
     */
    public static final String BOOT_STACK_SIZE_PROPERTY = "jboss.boot.thread.stack.size";

    /**
     * The default stack size for the thread used to process boot operations.
     *
     * @see #BOOT_STACK_SIZE_PROPERTY
     */
    public static final int DEFAULT_BOOT_STACK_SIZE = 2 * 1024 * 1024;

    private static int getBootStackSize() {
        String prop = SecurityActions.getSystemProperty(BOOT_STACK_SIZE_PROPERTY);
        if (prop == null) {
            return  DEFAULT_BOOT_STACK_SIZE;
        } else {
            int base = 1;
            String multiple = prop;
            int lastIdx = prop.length() - 1;
            if (lastIdx > 0) {
                char last = prop.charAt(lastIdx);
                if ('k' == last || 'K' == last) {
                    multiple = prop.substring(0, lastIdx);
                    base = 1024;
                } else if ('m' == last || 'M' == last) {
                    multiple = prop.substring(0, lastIdx);
                    base = 1024 * 1024;
                }
            }
            try {
                return Integer.parseInt(multiple) * base;
            } catch (NumberFormatException e) {
                ROOT_LOGGER.invalidSystemPropertyValue(prop, BOOT_STACK_SIZE_PROPERTY, DEFAULT_BOOT_STACK_SIZE);
                return DEFAULT_BOOT_STACK_SIZE;
            }
        }
    }

    protected final ProcessType processType;
    protected final DelegatingConfigurableAuthorizer authorizer;
    private final RunningModeControl runningModeControl;
    private final DescriptionProvider rootDescriptionProvider;
    private final ResourceDefinition rootResourceDefinition;
    private final ControlledProcessState processState;
    private final OperationStepHandler prepareStep;
    private final InjectedValue<ExecutorService> injectedExecutorService = new InjectedValue<ExecutorService>();
    private final ExpressionResolver expressionResolver;
    private volatile ModelControllerImpl controller;
    private ConfigurationPersister configurationPersister;
    private final ManagedAuditLogger auditLogger;

    /**
     * Construct a new instance.
     *
     * @param processType             the type of process being controlled
     * @param runningModeControl      the controller of the process' running mode
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     * @param auditLogger             the audit logger
     */
    @Deprecated
    protected AbstractControllerService(final ProcessType processType, final RunningModeControl runningModeControl,
                                        final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver,
                                        final ManagedAuditLogger auditLogger, DelegatingConfigurableAuthorizer authorizer) {
        this(processType, runningModeControl, configurationPersister, processState, null, rootDescriptionProvider,
                prepareStep, expressionResolver, auditLogger, authorizer);

    }

    /**
     * Construct a new instance.
     *
     * @param processType             the type of process being controlled
     * @param runningModeControl      the controller of the process' running mode
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootResourceDefinition  the root resource definition
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     * @param auditLogger             the audit logger
     */
    protected AbstractControllerService(final ProcessType processType, final RunningModeControl runningModeControl,
                                        final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final ResourceDefinition rootResourceDefinition,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver,
                                        final ManagedAuditLogger auditLogger, final DelegatingConfigurableAuthorizer authorizer) {
        this(processType, runningModeControl, configurationPersister, processState, rootResourceDefinition, null,
                prepareStep, expressionResolver, auditLogger, authorizer);
    }

    /**
     * Construct a new instance.
     * Simplified constructor for test case subclasses.
     *
     * @param processType             the type of process being controlled
     * @param runningModeControl      the controller of the process' running mode
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     *
     * @deprecated Here for backwards compatibility for ModelTestModelControllerService
     */
    @Deprecated
    protected AbstractControllerService(final ProcessType processType, final RunningModeControl runningModeControl,
                                        final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver) {
        this(processType, runningModeControl, configurationPersister, processState, null, rootDescriptionProvider,
                prepareStep, expressionResolver, AuditLogger.NO_OP_LOGGER, new DelegatingConfigurableAuthorizer());

    }

    /**
     * Construct a new instance.
     *
     * @param processType             the type of process being controlled
     * @param runningModeControl      the controller of the process' running mode
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootResourceDefinition  the root resource definition
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     *
     * @deprecated Here for backwards compatibility for ModelTestModelControllerService
     */
    protected AbstractControllerService(final ProcessType processType, final RunningModeControl runningModeControl,
                                        final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final ResourceDefinition rootResourceDefinition,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver) {
        this(processType, runningModeControl, configurationPersister, processState, rootResourceDefinition, null,
                prepareStep, expressionResolver, AuditLogger.NO_OP_LOGGER, new DelegatingConfigurableAuthorizer());
    }

    private AbstractControllerService(final ProcessType processType, final RunningModeControl runningModeControl,
                                      final ConfigurationPersister configurationPersister, final ControlledProcessState processState,
                                      final ResourceDefinition rootResourceDefinition, final DescriptionProvider rootDescriptionProvider,
                                      final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver, final ManagedAuditLogger auditLogger,
                                      final DelegatingConfigurableAuthorizer authorizer) {
        assert rootDescriptionProvider != null || rootResourceDefinition != null: rootDescriptionProvider == null ? "Null root description provider" : "Null root resource definition";
        assert expressionResolver != null : "Null expressionResolver";
        assert auditLogger != null : "Null auditLogger";
        assert authorizer != null : "Null authorizer";
        this.processType = processType;
        this.runningModeControl = runningModeControl;
        this.configurationPersister = configurationPersister;
        this.rootDescriptionProvider = rootDescriptionProvider;
        this.rootResourceDefinition = rootResourceDefinition;
        this.processState = processState;
        this.prepareStep = prepareStep;
        this.expressionResolver = expressionResolver;
        this.auditLogger = auditLogger;
        this.authorizer = authorizer;
    }

    public void start(final StartContext context) throws StartException {

        if (configurationPersister == null) {
            throw MESSAGES.persisterNotInjected();
        }
        final ServiceController<?> serviceController = context.getController();
        final ServiceContainer container = serviceController.getServiceContainer();
        final ServiceTarget target = context.getChildTarget();
        final ExecutorService executorService = injectedExecutorService.getOptionalValue();
        ManagementResourceRegistration rootResourceRegistration = rootDescriptionProvider != null
                ? ManagementResourceRegistration.Factory.create(rootDescriptionProvider, authorizer.getWritableAuthorizerConfiguration())
                : ManagementResourceRegistration.Factory.create(rootResourceDefinition, authorizer.getWritableAuthorizerConfiguration());
        final ModelControllerImpl controller = new ModelControllerImpl(container, target,
                rootResourceRegistration,
                new ContainerStateMonitor(container, serviceController),
                configurationPersister, processType, runningModeControl, prepareStep,
                processState, executorService, expressionResolver, authorizer, auditLogger);

        initModel(controller.getRootResource(), controller.getRootRegistration());
        this.controller = controller;

        final long bootStackSize = getBootStackSize();
        final Thread bootThread = new Thread(null, new Runnable() {
            public void run() {
                try {
                    try {
                        boot(new BootContext() {
                            public ServiceTarget getServiceTarget() {
                                return target;
                            }
                        });
                    } finally {
                        processState.setRunning();
                    }
                } catch (Throwable t) {
                    container.shutdown();
                    if (t instanceof StackOverflowError) {
                        ROOT_LOGGER.errorBootingContainer(t, bootStackSize, BOOT_STACK_SIZE_PROPERTY);
                    } else {
                        ROOT_LOGGER.errorBootingContainer(t);
                    }
                } finally {
                    bootThreadDone();
                }

            }
        }, "Controller Boot Thread", bootStackSize);
        bootThread.start();
    }

    /**
     * Boot the controller.  Called during service start.
     *
     * @param context the boot context
     * @throws ConfigurationPersistenceException
     *          if the configuration failed to be loaded
     */
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {
        runPerformControllerInitialization(context);
        boot(configurationPersister.load(), false);
        finishBoot();
    }

    protected boolean boot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) throws ConfigurationPersistenceException {
        return controller.boot(bootOperations, OperationMessageHandler.logging, ModelController.OperationTransactionControl.COMMIT, rollbackOnRuntimeFailure);
    }

    protected ModelNode internalExecute(final ModelNode operation, final OperationMessageHandler handler, final ModelController.OperationTransactionControl control, final OperationAttachments attachments, final OperationStepHandler prepareStep) {
        return controller.internalExecute(operation, handler, control, attachments, prepareStep);
    }

    /**
     * @deprecated internal use only
     */
    @Deprecated
    protected ModelNode executeReadOnlyOperation(final ModelNode operation, final OperationMessageHandler handler, final ModelController.OperationTransactionControl control, final OperationAttachments attachments, final OperationStepHandler prepareStep, int lockPermit) {
        return controller.executeReadOnlyOperation(operation, handler, control, attachments, prepareStep, lockPermit);
    }

    protected void finishBoot() throws ConfigurationPersistenceException {
        controller.finishBoot();
        configurationPersister.successfulBoot();
    }

    protected void bootThreadDone() {

    }

    public void stop(final StopContext context) {
        controller = null;

        context.asynchronous();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    stopAsynchronous(context);
                } finally {
                    try {
                        authorizer.shutdown();
                    } finally {
                        context.complete();
                    }
                }
            }
        };
        ExecutorService executorService = injectedExecutorService.getOptionalValue();
        if (executorService != null) {
            injectedExecutorService.getValue().execute(r);
        } else {
            Thread executorShutdown = new Thread(r, getClass().getSimpleName() + " Shutdown Thread");
            executorShutdown.start();
        }
    }

    /**
     * Hook for subclasses to perform work during the asynchronous task started by
     * {@link #stop(org.jboss.msc.service.StopContext)}. This base method does nothing.
     * <p><strong>Subclasses must not invoke {@link org.jboss.msc.service.StopContext#complete()}</strong></p>
     * @param context the stop context
     */
    protected void stopAsynchronous(StopContext context) {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @throws SecurityException if the caller does not have {@link ModelController#ACCESS_PERMISSION}
     */
    public ModelController getValue() throws IllegalStateException, IllegalArgumentException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ModelController.ACCESS_PERMISSION);
        }
        final ModelController controller = this.controller;
        if (controller == null) {
            throw new IllegalStateException();
        }
        return controller;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjector() {
        return injectedExecutorService;
    }

    protected void setConfigurationPersister(final ConfigurationPersister persister) {
        this.configurationPersister = persister;
    }

    protected void runPerformControllerInitialization(BootContext context) {
        performControllerInitialization(context.getServiceTarget(), controller.getRootResource(), controller.getRootRegistration());
    }

    protected void performControllerInitialization(ServiceTarget target, Resource rootResource, ManagementResourceRegistration rootRegistration) {
        //
    }

    protected abstract void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration);

    protected ManagedAuditLogger getAuditLogger() {
        return auditLogger;
    }
}

