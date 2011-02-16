/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.protocol.MessageHandler;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;

/**
 * Handles the remote operations for the model controller
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ModelControllerOperationHandler extends MessageHandler, ManagementOperationHandler {

    public class Factory {
        /**
         * Creates a new ModelControllerOperationHandler intended for use when new connections are created per request
         *
         * @param type the type of connection
         * @param modelController the target controller
         */
        public static ModelControllerOperationHandlerImpl create(ModelControllerClient.Type type, ModelController modelController) {
            return create(type, modelController, null);
        }

        /**
         * Creates a new ModelControllerOperationHandler intended for use when the connection is reused between requests
         *
         * @param type the type of connection
         * @param modelController the target controller
         */
        public static ModelControllerOperationHandlerImpl create(ModelControllerClient.Type type, ModelController modelController, MessageHandler initialMessageHandler) {
            if (initialMessageHandler == null) {
                initialMessageHandler = MessageHandler.NULL;
            }
            return new ModelControllerOperationHandlerImpl(type, modelController, initialMessageHandler);
        }
    }
}
