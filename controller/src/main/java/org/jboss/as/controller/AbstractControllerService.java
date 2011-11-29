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

import java.util.List;
import java.util.concurrent.ExecutorService;

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

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

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

    private final OperationContextTypeFactory contextTypeFactory;
    private final DescriptionProvider rootDescriptionProvider;
    private final ControlledProcessState processState;
    private final OperationStepHandler prepareStep;
    private final InjectedValue<ExecutorService> injectedExecutorService = new InjectedValue<ExecutorService>();
    private final ExpressionResolver expressionResolver;
    private volatile ModelControllerImpl controller;
    private ConfigurationPersister configurationPersister;

    /**
     * Construct a new instance.
     *
     * @param controllerType          the controller type for the new controller
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     *
     * @deprecated Use one of the other constructor variants
     */
    @Deprecated
    protected AbstractControllerService(final OperationContext.Type controllerType,
                                        final ControlledProcessState processState,
                                        final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep) {
        this(new OperationContextTypeFactory.SimpleTypeFactory(controllerType), null, processState, rootDescriptionProvider, prepareStep, null);
    }

    /**
     * Construct a new instance.
     *
     * @param controllerType          the controller type for the new controller
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     */
    protected AbstractControllerService(final OperationContext.Type controllerType,
                                        final ControlledProcessState processState,
                                        final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep,
                                        final ExpressionResolver expressionResolver) {
        this(new OperationContextTypeFactory.SimpleTypeFactory(controllerType), null, processState, rootDescriptionProvider, prepareStep, expressionResolver);
    }

    /**
     * Construct a new instance.
     *
     * @param controllerType          the controller type for the new controller
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     */
    protected AbstractControllerService(final OperationContext.Type controllerType, final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver) {
        this(new OperationContextTypeFactory.SimpleTypeFactory(controllerType), configurationPersister, processState, rootDescriptionProvider,
                prepareStep, expressionResolver);
    }

    /**
     * Construct a new instance.
     *
     * @param contextTypeFactory      the factory for the type of operation context the new controller should create
     * @param configurationPersister  the configuration persister
     * @param processState            the controlled process state
     * @param rootDescriptionProvider the root description provider
     * @param prepareStep             the prepare step to prepend to operation execution
     * @param expressionResolver      the expression resolver
     */
    protected AbstractControllerService(final OperationContextTypeFactory contextTypeFactory, final ConfigurationPersister configurationPersister,
                                        final ControlledProcessState processState, final DescriptionProvider rootDescriptionProvider,
                                        final OperationStepHandler prepareStep, final ExpressionResolver expressionResolver) {
        this.contextTypeFactory = contextTypeFactory;
        this.configurationPersister = configurationPersister;
        this.rootDescriptionProvider = rootDescriptionProvider;
        this.processState = processState;
        this.prepareStep = prepareStep;
        this.expressionResolver = expressionResolver != null ? expressionResolver : ExpressionResolver.DEFAULT;

    }


    public void start(final StartContext context) throws StartException {

        if (configurationPersister == null) {
            throw MESSAGES.persisterNotInjected();
        }
        final ServiceController<?> serviceController = context.getController();
        final ServiceContainer container = serviceController.getServiceContainer();
        final ServiceTarget target = context.getChildTarget();
        final ExecutorService executorService = injectedExecutorService.getOptionalValue();
        final ModelControllerImpl controller = new ModelControllerImpl(container, target,
                ManagementResourceRegistration.Factory.create(rootDescriptionProvider),
                new ContainerStateMonitor(container, serviceController),
                configurationPersister, contextTypeFactory, prepareStep,
                processState, executorService, expressionResolver);
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
                    } catch (ConfigurationPersistenceException e) {
                        throw new RuntimeException(e);
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
        boot(configurationPersister.load());
        finishBoot();
    }

    protected void boot(List<ModelNode> bootOperations) throws ConfigurationPersistenceException {
        controller.boot(bootOperations, OperationMessageHandler.logging, ModelController.OperationTransactionControl.COMMIT);
    }

    protected void finishBoot() throws ConfigurationPersistenceException {
        controller.finishBoot();
        configurationPersister.successfulBoot();
    }

    public void stop(final StopContext context) {
        controller = null;
    }

    public ModelController getValue() throws IllegalStateException, IllegalArgumentException {
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

    protected abstract void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration);
}
