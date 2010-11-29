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

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceName;

/**
 * The JMS subsystem update. Creating and installing the core subsystem services.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSSubsystemAdd extends AbstractSubsystemAdd<JMSSubsystemElement> {

    private static final long serialVersionUID = 8476951371785341747L;
    // Dependency on the JNDI service
    static final ServiceName JNDI_SERVICE_NAME = ServiceName.JBOSS.append("naming", "context", "java");

    public JMSSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // FIXME the JMSServer is started as part of the messaging subsystem for now
        // final BatchBuilder builder = context.getBatchBuilder();
        // final JMSService service = new JMSService();
        // final ServiceBuilder<?> serviceBuilder = builder.addService(JMSSubsystemElement.JMS_MANAGER, service)
        //    .addDependency(MessagingSubsystemElement.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQServer())
        //    .addOptionalDependency(JNDI_SERVICE_NAME);
        // serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
    }

    /** {@inheritDoc} */
    protected JMSSubsystemElement createSubsystemElement() {
        return new JMSSubsystemElement();
    }

}
