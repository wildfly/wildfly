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
import org.hornetq.core.server.HornetQServer;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.jboss.as.messaging.HornetQDefaultCredentials;
import org.jboss.msc.service.Service;
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

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {
    private final InjectedValue<HornetQServer> hornetQServer = new InjectedValue<HornetQServer>();
    private JMSServerManager jmsServer;

    public static ServiceController<?> addService(final ServiceTarget target, ServiceName hqServiceName, final ServiceListener<Object>... listeners) {
        final JMSService service = new JMSService();
        return target.addService(JMSServices.getJmsManagerBaseServiceName(hqServiceName), service)
            .addDependency(hqServiceName, HornetQServer.class, service.getHornetQServer())
            .addListener(listeners)
            .setInitialMode(Mode.ACTIVE)
            .install();
    }

    protected JMSService() {
        //
    }

    public synchronized void start(StartContext context) throws StartException {
        try {
            final JMSServerManager jmsServer = new JMSServerManagerImpl(hornetQServer.getValue(), new AS7BindingRegistry(context.getController().getServiceContainer()));

            try {
                // FIXME - we also need the TCCL here in case the JMSServerManager starts the HornetQServer
                final ClassLoader loader = getClass().getClassLoader();
                SecurityActions.setContextClassLoader(loader);
                jmsServer.start();

                // FIXME - this check is a work-around for AS7-3658
                if (!hornetQServer.getValue().getConfiguration().isBackup()) {
                    hornetQServer.getValue().getRemotingService().allowInvmSecurityOverride(new HornetQPrincipal(HornetQDefaultCredentials.getUsername(), HornetQDefaultCredentials.getPassword()));
                }
            } finally {
                SecurityActions.setContextClassLoader(null);
            }
            this.jmsServer = jmsServer;
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    public synchronized void stop(StopContext context) {
        final JMSServerManager jmsServer = this.jmsServer;
        this.jmsServer = null;
        try {
            jmsServer.stop();
        } catch (Exception e) {
            MESSAGING_LOGGER.errorStoppingJmsServer(e);
        }
    }

    public synchronized JMSServerManager getValue() throws IllegalStateException {
        final JMSServerManager jmsServer = this.jmsServer;
        if(jmsServer == null) {
            throw new IllegalStateException();
        }
        return jmsServer;
    }

    InjectedValue<HornetQServer> getHornetQServer() {
        return hornetQServer;
    }
}
