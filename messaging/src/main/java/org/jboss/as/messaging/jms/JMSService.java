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

import org.hornetq.core.server.HornetQServer;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.naming.MockContext;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.CompositeName;
import javax.naming.Name;
import org.omg.IOP.ServiceContext;

/**
 * The {@code JMSServerManager} service.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSService implements Service<JMSServerManager> {
    private final InjectedValue<NamingStore> namingStore = new InjectedValue<NamingStore>();
    private final InjectedValue<HornetQServer> hornetQServer = new InjectedValue<HornetQServer>();
    private JMSServerManager jmsServer;

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        final JMSService service = new JMSService();
        return target.addService(JMSServices.JMS_MANAGER, service)
            .addDependency(MessagingServices.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQServer())
            .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, service.getNamingStore())
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
            Logger.getLogger("org.jboss.messaging").error("exception while stopping jms server", e);
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

    Injector<NamingStore> getNamingStore() {
        return namingStore;
    }
}
