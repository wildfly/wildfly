/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.service;

import static org.jboss.as.appclient.logging.AppClientLogger.ROOT_LOGGER;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;


/**
 * Service that is responsible for running an application clients main method, and shutting down the server once it
 * completes
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStartService implements Service {


    public static final ServiceName SERVICE_NAME = ServiceName.of("appClientStart");

    private final Supplier<ApplicationClientDeploymentService> applicationClientDeploymentServiceSupplier;
    private final Supplier<Component> applicationClientComponentSupplier;
    private final InjectedEENamespaceContextSelector namespaceContextSelectorInjectedValue;
    private final List<SetupAction> setupActions;
    private final Method mainMethod;
    private final String[] parameters;
    private final ClassLoader classLoader;

    private Thread thread;
    private ComponentInstance instance;

    public ApplicationClientStartService(final Method mainMethod, final String[] parameters,
                                         final InjectedEENamespaceContextSelector namespaceContextSelectorInjectedValue,
                                         final ClassLoader classLoader,  final List<SetupAction> setupActions,
                                         final Supplier<ApplicationClientDeploymentService> applicationClientDeploymentServiceSupplier,
                                         final Supplier<Component> applicationClientComponentSupplier) {
        this.mainMethod = mainMethod;
        this.parameters = parameters;
        this.namespaceContextSelectorInjectedValue = namespaceContextSelectorInjectedValue;
        this.classLoader = classLoader;
        this.setupActions = setupActions;
        this.applicationClientComponentSupplier = applicationClientComponentSupplier;
        this.applicationClientDeploymentServiceSupplier = applicationClientDeploymentServiceSupplier;
    }
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                final ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                        Throwable originalException = null;
                        try {
                            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                            applicationClientDeploymentServiceSupplier.get().getDeploymentCompleteLatch().await();
                            NamespaceContextSelector.setDefault(namespaceContextSelectorInjectedValue);

                            try {
                                //perform any additional setup that may be needed
                                for (SetupAction action : setupActions) {
                                    action.setup(Collections.<String, Object>emptyMap());
                                }

                                //do static injection etc
                                instance = applicationClientComponentSupplier.get().createInstance();

                                mainMethod.invoke(null, new Object[]{parameters});
                            } catch (Throwable ex) {
                                if (ex instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                }
                                originalException = ex;
                            } finally {
                                final ListIterator<SetupAction> iterator = setupActions.listIterator(setupActions.size());
                                Throwable suppressed = originalException;
                                while (iterator.hasPrevious()) {
                                    SetupAction action = iterator.previous();
                                    try {
                                        action.teardown(Collections.<String, Object>emptyMap());
                                    } catch (Throwable e) {
                                        if (suppressed != null) {
                                            suppressed.addSuppressed(e);
                                        } else {
                                            suppressed = e;
                                        }
                                    }
                                }
                                if (suppressed != null) {
                                    if (suppressed instanceof RuntimeException) {
                                        throw (RuntimeException) suppressed;
                                    }
                                    if (suppressed instanceof Error) {
                                        throw (Error) suppressed;
                                    }
                                    throw new RuntimeException(suppressed);
                                }
                            }
                        } catch (Exception e) {
                            ROOT_LOGGER.exceptionRunningAppClient(e, e.getClass().getSimpleName());
                        } finally {
                            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
                        }

                } finally {
                    serviceContainer.shutdown();
                }
            }
        });
        thread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if(serviceContainer != null) {
                    serviceContainer.shutdown();
                }
            }
        }));

    }

    @Override
    public synchronized void stop(final StopContext context) {
        if (instance != null) {
            instance.destroy();
        }
        thread.interrupt();
        thread = null;
    }

}
