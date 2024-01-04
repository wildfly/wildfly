/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.server.Services.addServerExecutorDependency;
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.REMOVE;
import static org.jboss.msc.service.ServiceController.State.REMOVED;
import static org.jboss.msc.service.ServiceController.State.STOPPING;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.security.ActiveMQPrincipal;
import org.apache.activemq.artemis.core.server.ActivateCallback;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.jms.server.impl.JMSServerManagerImpl;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;
import org.wildfly.extension.messaging.activemq.ActiveMQBroker;
import org.wildfly.extension.messaging.activemq.DefaultCredentials;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {
    private final InjectedValue<ActiveMQBroker> activeMQServer = new InjectedValue<>();
    private final InjectedValue<ExecutorService> serverExecutor = new InjectedValue<>();
    private final ServiceName serverServiceName;
    private final boolean overrideInVMSecurity;
    private JMSServerManager jmsServer;

    public static ServiceController<JMSServerManager> addService(final ServiceTarget target, ServiceName serverServiceName, boolean overrideInVMSecurity) {
        final JMSService service = new JMSService(serverServiceName, overrideInVMSecurity);
        ServiceBuilder<JMSServerManager> builder = target.addService(JMSServices.getJmsManagerBaseServiceName(serverServiceName), service);
        builder.addDependency(serverServiceName, ActiveMQBroker.class, service.activeMQServer);
        builder.requires(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL);
        builder.setInitialMode(Mode.ACTIVE);
        addServerExecutorDependency(builder, service.serverExecutor);
        return builder.install();
    }

    protected JMSService(ServiceName serverServiceName, boolean overrideInVMSecurity) {
        this.serverServiceName = serverServiceName;
        this.overrideInVMSecurity = overrideInVMSecurity;
    }

    public synchronized JMSServerManager getValue() throws IllegalStateException {
        if (jmsServer == null) {
            throw new IllegalStateException();
        }
        return jmsServer;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    doStart(context);
                    context.complete();
                } catch (StartException e) {
                    context.failed(e);
                }
            }
        };
        try {
            serverExecutor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }


    @Override
    public void stop(final StopContext context) {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                doStop(context);
                context.complete();
            }
        };
        try {
            serverExecutor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    private synchronized void doStart(final StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        ClassLoader oldTccl = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(getClass());
        try {
            jmsServer = new JMSServerManagerImpl(ActiveMQServer.class.cast(activeMQServer.getValue().getDelegate()), new WildFlyBindingRegistry(context.getController().getServiceContainer())) {
                @Override
                public void stop(ActiveMQServer server) {
                    // Suppress ARTEMIS-2438
                }
            };

            ActiveMQServer.class.cast(activeMQServer.getValue().getDelegate()).registerActivationFailureListener(e -> {
                StartException se = new StartException(e);
                context.failed(se);
            });
            ActiveMQServer.class.cast(activeMQServer.getValue().getDelegate()).registerActivateCallback(new ActivateCallback() {
                private volatile ServiceController<Void> activeMQActivationController;

                public void preActivate() {
                }

                public void activated() {
                    if (overrideInVMSecurity) {
                        ActiveMQServer.class.cast(activeMQServer.getValue().getDelegate()).getRemotingService().allowInvmSecurityOverride(new ActiveMQPrincipal(DefaultCredentials.getUsername(), DefaultCredentials.getPassword()));
                    }
                    // ActiveMQ only provides a callback to be notified when ActiveMQ core server is activated.
                    // but the Jakarta Messaging service start must not be completed until the JMSServerManager wrappee is indeed started (and has deployed the Jakarta Messaging resources, etc.).
                    // It is possible that the activation service has already been installed but becomes passive when a backup server has failed over (-> ACTIVE) and failed back (-> PASSIVE)
                    // [WFLY-6178] check if the service container is shutdown to avoid an IllegalStateException if an
                    //   ActiveMQ backup server is activated during failover while the WildFly server is shutting down.
                    if (serviceContainer.isShutdown()) {
                        return;
                    }
                    if (activeMQActivationController == null) {
                        activeMQActivationController = serviceContainer.addService(ActiveMQActivationService.getServiceName(serverServiceName), new ActiveMQActivationService())
                                .setInitialMode(Mode.ACTIVE)
                                .install();
                    } else {
                        activeMQActivationController.setMode(ACTIVE);
                    }
                }

                @Override
                public void activationComplete() {

                }

                public void deActivate() {
                    // passivate the activation service only if the ActiveMQ server is deactivated when it fails back
                    // and *not* during AS7 service container shutdown or reload (AS7-6840 / AS7-6881)
                    if (activeMQActivationController != null
                            && !activeMQActivationController.getState().in(STOPPING, REMOVED)) {
                        // [WFLY-4597] When Artemis is deactivated during failover, we block until its
                        // activation controller is REMOVED before giving back control to Artemis.
                        // This allow to properly stop any service depending on the activation controller
                        // and avoid spurious warning messages because the resources used by the services
                        // are stopped outside the control of the services.
                        final CountDownLatch latch = new CountDownLatch(1);
                        activeMQActivationController.compareAndSetMode(ACTIVE, REMOVE);
                        activeMQActivationController.addListener(new LifecycleListener() {
                            @Override
                            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                                if (event == LifecycleEvent.REMOVED) {
                                    latch.countDown();
                                }
                            }
                        });
                        try {
                            latch.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                        }
                        activeMQActivationController = null;
                    }
                }
            });
            jmsServer.start();
        } catch(StartException e){
            throw e;
        } catch (Throwable t) {
            throw new StartException(t);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    private synchronized void doStop(StopContext context) {
        try {
            jmsServer.stop();
            jmsServer = null;
        } catch (Exception e) {
            MessagingLogger.ROOT_LOGGER.errorStoppingJmsServer(e);
        }
    }
}
