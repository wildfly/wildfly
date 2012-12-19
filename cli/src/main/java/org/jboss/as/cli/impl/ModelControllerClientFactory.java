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
package org.jboss.as.cli.impl;

import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * @author Alexey Loubyansky
 *
 */
public interface ModelControllerClientFactory {

    interface ConnectionCloseHandler {
        void handleClose();
    }

    ModelControllerClient getClient(String hostName, int port, CallbackHandler handler,
            SSLContext sslContext, int connectionTimeout, ConnectionCloseHandler closeHandler) throws IOException;

    ModelControllerClientFactory DEFAULT = new ModelControllerClientFactory() {
        @Override
        public ModelControllerClient getClient(String hostName, int port, CallbackHandler handler,
                SSLContext sslContext, int connectionTimeout, ConnectionCloseHandler closeHandler) throws IOException {
            return ModelControllerClient.Factory.create(hostName, port, handler, sslContext, connectionTimeout);
        }
    };

    ModelControllerClientFactory CUSTOM = new ModelControllerClientFactory() {

        @Override
        public ModelControllerClient getClient(final String hostName, final int port,
                final CallbackHandler handler, final SSLContext sslContext,
                final int connectionTimeout, final ConnectionCloseHandler closeHandler) throws IOException {

            return new CLIModelControllerClient(handler, hostName, connectionTimeout, closeHandler, port, sslContext);
        }};
}
