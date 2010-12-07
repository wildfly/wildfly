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
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Update adding a {@code ConfigurationFactoryElement} to the {@code JMSSubsystemElement}. The
 * runtime action, will create the {@code ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 */
public class ConnectionFactoryAdd extends AbstractJMSSubsystemUpdate<Void> {

    private static final long serialVersionUID = -629127809193102926L;
    private final ConnectionFactoryElement cf;

    public ConnectionFactoryAdd(ConnectionFactoryElement cf) {
        this.cf = cf;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JMSSubsystemElement element) throws UpdateFailedException {
        if(! element.addConnectionFactory(cf)) {
            throw new UpdateFailedException("duplicate connection-factory " + cf.getName());
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> handler, P param) {
        final ConnectionFactoryService service = new ConnectionFactoryService(transform());
        final ServiceName serviceName = JMSSubsystemElement.JMS_CF_BASE.append(cf.getName());
        context.getServiceTarget().addService(serviceName, service)
                .addDependency(JMSSubsystemElement.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                .addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param))
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JMSSubsystemElement, ?> getCompensatingUpdate(JMSSubsystemElement original) {
        return new ConnectionFactoryRemove(cf.getName());
    }

    /**
     * Transform our metadata to the HornetQ cf configuration
     *
     * @return the transformed configuration metadata
     */
    private ConnectionFactoryConfiguration transform() {
        return cf.transform();
    }

}
