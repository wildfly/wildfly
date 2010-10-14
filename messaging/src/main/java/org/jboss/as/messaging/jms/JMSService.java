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

import javax.naming.Context;

import org.hornetq.core.server.HornetQServer;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {

    private final InjectedValue<HornetQServer> hornetQServer = new InjectedValue<HornetQServer>();
    private final InjectedValue<Context> contextInjector = new InjectedValue<Context>();
    private JMSServerManager jmsServer;

    public static void addService(final BatchBuilder builder) {
        final JMSService service = new JMSService();
        builder.addService(JMSSubsystemElement.JMS_MANAGER, service)
            .addDependency(MessagingSubsystemElement.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQServer())
            .setInitialMode(Mode.IMMEDIATE);
    }

    protected JMSService() {
        //
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        try {
            final JMSServerManager jmsServer = new JMSServerManagerImpl(hornetQServer.getValue());

            final Context jndiContext = contextInjector.getOptionalValue();
            if(context != null) {
                jmsServer.setContext(jndiContext);
            }

            try {
                // FIXME - we also need the TCCL here in case the JMSServerManager starts the HornetQServer
                final ClassLoader loader = getClass().getClassLoader();
                SecurityActions.setContextClassLoader(loader);
                jmsServer.start();
            } finally {
                SecurityActions.setContextClassLoader(null);
            }
            this.jmsServer = jmsServer;
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final JMSServerManager jmsServer = this.jmsServer;
        this.jmsServer = null;
        try {
            jmsServer.stop();
        } catch (Exception e) {
            Logger.getLogger("org.jboss.messaging").error("exception while stopping jms server", e);
        }
    }

    /** {@inheritDoc} */
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

    InjectedValue<Context> getContextInjector() {
        return contextInjector;
    }
}
