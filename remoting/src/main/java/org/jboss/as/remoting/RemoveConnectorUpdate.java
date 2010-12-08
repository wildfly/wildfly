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

package org.jboss.as.remoting;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;


/**
 * An update which removes a connector from the remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemoveConnectorUpdate extends AbstractRemotingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -8965990593053845956L;

    private final String name;

    /**
     * Construct a new instance.
     *
     * @param name the name of the connector to remove
     */
    public RemoveConnectorUpdate(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> controller = updateContext.getServiceRegistry().getService(ConnectorElement.connectorName(name));
        if(controller == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<RemotingSubsystemElement, ?> getCompensatingUpdate(RemotingSubsystemElement subSystem) {
        final ConnectorElement original = subSystem.getConnector(name);
        if(original == null) {
            throw new IllegalStateException("null connector name");
        }
        final AddConnectorUpdate update = new AddConnectorUpdate(original.getName(), original.getSocketBinding());
        update.setAuthenticationProvider(original.getAuthenticationProvider());
        update.setSaslElement(original.getSaslElement());
        return update;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(RemotingSubsystemElement element) throws UpdateFailedException {
        ConnectorElement connector = element.removeConnector(name);
        if(connector == null) {
            throw new IllegalStateException("failed to remove connector " + name);
        }
    }

}
