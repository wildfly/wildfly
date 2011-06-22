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

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.naming.MockContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;

import java.util.Map;

/**
 * Service responsible for creating and destroying a {@code javax.jms.Queue}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSQueueService implements Service<Void> {

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();

    private String queueName;
    private String selectorString;
    private boolean durable;
    private String[] jndi;

    public JMSQueueService(final String queueName, String selectorString, boolean durable, String[] jndi) {
        this.queueName = queueName;
        this.selectorString = selectorString;
        this.durable = durable;
        this.jndi = jndi;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            jmsManager.createQueue(false, queueName, selectorString, durable, jndi);
        } catch (Exception e) {
            throw new StartException("failed to create queue", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            jmsManager.destroyQueue(queueName);
        } catch (Exception e) {
            Logger.getLogger("org.jboss.messaging").warnf(e ,"failed to destroy jms queue: %s", queueName);
        }
        // FIXME This shouldn't be here
        for(final String jndiBinding : jndi) {
            ServiceController<?> service = context.getController().getServiceContainer().getService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiBinding));
            if (service != null) {
                service.setMode(ServiceController.Mode.REMOVE);
            }
        }
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public InjectedValue<JMSServerManager> getJmsServer() {
        return jmsServer;
    }

}
