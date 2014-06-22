/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import org.hornetq.core.security.HornetQPrincipal;
import org.hornetq.core.server.ActivateCallback;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.jboss.as.messaging.HornetQActivationService;
import org.jboss.as.messaging.HornetQDefaultCredentials;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.messaging.logging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.server.Services.addServerExecutorDependency;
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.REMOVE;
import static org.jboss.msc.service.ServiceController.State.REMOVED;
import static org.jboss.msc.service.ServiceController.State.STOPPING;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {
    private final InjectedValue<HornetQServer> hornetQServer = new InjectedValue<HornetQServer>();
    private final InjectedValue<ExecutorService> serverExecutor = new InjectedValue<ExecutorService>();
    private final ServiceName hqServiceName;
    private final boolean overrideInVMSecurity;
    private JMSServerManager jmsServer;

    public static ServiceController<JMSServerManager> addService(final ServiceTarget target, ServiceName hqServiceName, boolean overrideInVMSecurity, final ServiceListener<Object>... listeners) {
        final JMSService service = new JMSService(hqServiceName, overrideInVMSecurity);
        ServiceBuilder<JMSServerManager> builder = target.addService(JMSServices.getJmsManagerBaseServiceName(hqServiceName), service)
                .addDependency(hqServiceName, HornetQServer.class, service.hornetQServer)
                .addListener(listeners)
                .setInitialMode(Mode.ACTIVE);
        addServerExecutorDependency(builder, service.serverExecutor, false);
        return builder.install();
    }

    protected JMSService(ServiceName hqServiceName, boolean overrideInVMSecurity) {
        this.hqServiceName = hqServiceName;
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
            jmsServer = new JMSServerManagerImpl(hornetQServer.getValue(), new AS7BindingRegistry(context.getController().getServiceContainer()));

            hornetQServer.getValue().registerActivateCallback(new ActivateCallback() {
                private volatile ServiceController<Void> hornetqActivationController;

                public void preActivate() {
                }

                public void activated() {
                    if (overrideInVMSecurity) {
                        hornetQServer.getValue().getRemotingService().allowInvmSecurityOverride(new HornetQPrincipal(HornetQDefaultCredentials.getUsername(), HornetQDefaultCredentials.getPassword()));
                    }
                    // HornetQ only provides a callback to be notified when HornetQ core server is activated.
                    // but the JMS service start must not be completed until the JMSServerManager wrappee is indeed started (and has deployed the JMS resources, etc.).
                    // It is possible that the activation service has already been installed but becomes passive when a backup server has failed over (-> ACTIVE) and failed back (-> PASSIVE)
                    if (hornetqActivationController == null) {
                        hornetqActivationController = serviceContainer.addService(HornetQActivationService.getHornetQActivationServiceName(hqServiceName), new HornetQActivationService())
                                .setInitialMode(Mode.ACTIVE)
                                .install();
                    } else {
                        hornetqActivationController.setMode(ACTIVE);
                    }
                }

                public void deActivate() {
                    // passivate the activation service only if the HornetQ server is deactivated when it fails back
                    // and *not* during AS7 service container shutdown or reload (AS7-6840 / AS7-6881)
                    if (hornetqActivationController != null) {
                        if (!hornetqActivationController.getState().in(STOPPING, REMOVED)) {
                            hornetqActivationController.compareAndSetMode(ACTIVE, REMOVE);
                            hornetqActivationController = null;
                        }
                    }
                }
            });
            jmsServer.start();
        } catch(StartException e){
            throw e;
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.failedToStartService(e);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    private synchronized void doStop(StopContext context) {
        try {
            jmsServer.stop();
            jmsServer = null;
        } catch (Exception e) {
            MESSAGING_LOGGER.errorStoppingJmsServer(e);
        }
    }
}
