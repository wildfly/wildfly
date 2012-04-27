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

package org.jboss.as.controller.remote;

import org.jboss.as.controller.ModelController;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;

/**
 * @author Emanuel Muckenhuber
 */
public final class TransactionalProtocolHandlers {

    private TransactionalProtocolHandlers() {
        //
    }

    /**
     * Create a transactional protocol client.
     *
     * @param channelAssociation the channel handler
     * @return the transactional protocol client
     */
    public static TransactionalProtocolClient createClient(final ManagementChannelHandler channelAssociation) {
        final TransactionalProtocolClientImpl client = new TransactionalProtocolClientImpl(channelAssociation);
        channelAssociation.addHandlerFactory(client);
        return client;
    }

    /**
     * Add a transaction protocol request handler to an existing channel.
     *
     * @param association the channel association
     * @param controller the model controller
     */
    public static void addAsHandlerFactory(final ManagementChannelHandler association, final ModelController controller) {
        final ManagementRequestHandlerFactory handlerFactory = createHandler(association, controller);
        association.addHandlerFactory(handlerFactory);
    }

    /**
     * Create a transactional protocol request handler.
     *
     * @param association the management channel
     * @param controller the model controller
     * @return the handler factory
     */
    public static ManagementRequestHandlerFactory createHandler(final ManagementChannelAssociation association, final ModelController controller) {
        return new TransactionalProtocolOperationHandler(controller, association);
    }

}
