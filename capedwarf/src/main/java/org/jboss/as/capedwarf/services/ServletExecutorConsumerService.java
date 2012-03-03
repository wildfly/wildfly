/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

/**
 * Servlet executor consumera service
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutorConsumerService implements Service<Connection> {

    public static final ServiceName NAME = ServiceName.JBOSS.append("capedwarf").append("consumer");

    private Logger log = Logger.getLogger(ServletExecutorConsumerService.class);

    private InjectedValue<ManagedReferenceFactory> factory = new InjectedValue<ManagedReferenceFactory>();
    private InjectedValue<ManagedReferenceFactory> queue = new InjectedValue<ManagedReferenceFactory>();
    private InjectedValue<ModuleLoader> loader = new InjectedValue<ModuleLoader>();

    private ServletExecutorConsumer sec;
    private Connection connection;

    private static <T> T cast(Class<T> clazz, ManagedReferenceFactory mrf) {
        return clazz.cast(mrf.getReference().getInstance());
    }

    public void start(StartContext context) throws StartException {
        try {
            final Connection qc = cast(ConnectionFactory.class, factory.getValue()).createConnection();
            final Session session = qc.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer consumer = session.createConsumer(cast(Queue.class, queue.getValue()));
            sec = new ServletExecutorConsumer(loader.getValue());
            consumer.setMessageListener(sec);
            qc.start();
            connection = qc;
        } catch (Exception e) {
            throw new StartException("Cannot start JMS connection.", e);
        }
    }

    public void stop(StopContext context) {
        try {
            connection.stop();
        } catch (JMSException e) {
            log.error("Error stopping JMS connection.", e);
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                log.warn("Error closing JMS connection.", e);
            }
        }
    }

    public void removeModule(Module module) {
        if (sec != null) {
            sec.removeClassLoader(module.getClassLoader());
        }
    }

    public Connection getValue() throws IllegalStateException, IllegalArgumentException {
        return connection;
    }

    public InjectedValue<ManagedReferenceFactory> getFactory() {
        return factory;
    }

    public InjectedValue<ManagedReferenceFactory> getQueue() {
        return queue;
    }

    public InjectedValue<ModuleLoader> getLoader() {
        return loader;
    }
}
