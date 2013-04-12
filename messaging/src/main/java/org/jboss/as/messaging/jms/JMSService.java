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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.messaging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.PASSIVE;
import static org.jboss.msc.service.ServiceController.Mode.REMOVE;
import static org.jboss.msc.service.ServiceController.State.REMOVED;
import static org.jboss.msc.service.ServiceController.State.STOPPING;

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {
    private final InjectedValue<HornetQServer> hornetQServer = new InjectedValue<HornetQServer>();
    private final ServiceName hqServiceName;
    private JMSServerManager jmsServer;

    public static ServiceController<JMSServerManager> addService(final ServiceTarget target, ServiceName hqServiceName, final ServiceListener<Object>... listeners) {
        final JMSService service = new JMSService(hqServiceName);
        return target.addService(JMSServices.getJmsManagerBaseServiceName(hqServiceName), service)
                .addDependency(hqServiceName, HornetQServer.class, service.getHornetQServer())
                .addListener(listeners)
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    protected JMSService(ServiceName hqServiceName) {
        this.hqServiceName = hqServiceName;
    }

    public synchronized void start(final StartContext context) throws StartException {
        ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(getClass());
        try {
            jmsServer = new JMSServerManagerImpl(hornetQServer.getValue(), new AS7BindingRegistry(context.getController().getServiceContainer()));
            final ServiceBuilder<Void> hornetqActivationService = context.getChildTarget().addService(HornetQActivationService.getHornetQActivationServiceName(hqServiceName), new HornetQActivationService())
                    .setInitialMode(Mode.ACTIVE);

            hornetQServer.getValue().registerActivateCallback(new ActivateCallback() {
                private volatile ServiceController<Void> hornetqActivationController;

                public void preActivate() {
                }

                public void activated() {
                    // FIXME - this check is a work-around for AS7-3658
                    hornetQServer.getValue().getRemotingService().allowInvmSecurityOverride(new HornetQPrincipal(HornetQDefaultCredentials.getUsername(), HornetQDefaultCredentials.getPassword()));
                    // HornetQ only provides a callback to be notified when HornetQ core server is activated.
                    // but the JMS service start must not be completed until the JMSServerManager wrappee is indeed started (and has deployed the JMS resources, etc.).
                    // It is possible that the activation service has already been installed but becomes passive when a backup server has failed over (-> ACTIVE) and failed back (-> PASSIVE)
                    if (hornetqActivationController == null) {
                        hornetqActivationController = hornetqActivationService.install();
                    } else {
                        hornetqActivationController.setMode(ACTIVE);
                    }
                }

                public void deActivate() {
                    // passivate the activation service only if the HornetQ server is deactivated when it fails back
                    // and *not* during AS7 service container shutdown or reload (AS7-6840 / AS7-6881)
                    if (hornetqActivationController != null) {
                        if (hornetqActivationController.getState() != STOPPING &&
                                hornetqActivationController.getState() != REMOVED) {
                            hornetqActivationController.compareAndSetMode(ACTIVE, PASSIVE);
                        }
                    }
                }
            });
            jmsServer.start();
        } catch(StartException e){
            throw e;
        } catch (Exception e) {
            throw MESSAGES.failedToStartService(e);
        } finally {
            SecurityActions.setThreadContextClassLoader(oldTccl);
        }
    }

    public synchronized void stop(StopContext context) {
        try {
            jmsServer.stop();
            jmsServer = null;
        } catch (Exception e) {
            MESSAGING_LOGGER.errorStoppingJmsServer(e);
        }
    }

    public synchronized JMSServerManager getValue() throws IllegalStateException {
        if (jmsServer == null) {
            throw new IllegalStateException();
        }
        return jmsServer;
    }

    InjectedValue<HornetQServer> getHornetQServer() {
        return hornetQServer;
    }
}
