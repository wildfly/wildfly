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

package org.jboss.as.jmx;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
public class JMXConnectorRemove extends AbstractSubsystemUpdate<JmxSubsystemElement, Void> {

    private static final long serialVersionUID = 5839017395274175195L;

    protected JMXConnectorRemove() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JmxSubsystemElement element) throws UpdateFailedException {
        element.setConnector(null);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = context.getServiceContainer().getService(JMXConnectorService.SERVICE_NAME);
        if(service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JmxSubsystemElement, ?> getCompensatingUpdate(JmxSubsystemElement original) {
        final JMXConnectorElement connector = original.getConnector();
        if(connector == null) {
            return null;
        }
        return new JMXConnectorAdd(connector.getServerBinding(), connector.getRegistryBinding());
    }

    /** {@inheritDoc} */
    public Class<JmxSubsystemElement> getModelElementType() {
        return JmxSubsystemElement.class;
    }

}
