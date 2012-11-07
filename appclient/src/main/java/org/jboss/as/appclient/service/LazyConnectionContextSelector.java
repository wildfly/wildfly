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

package org.jboss.as.appclient.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Selector that does not create a connection until it is actually required.
 * <p/>
 * This allows the user to perform a login before the connection is established.
 *
 * @author Stuart Douglas
 */
public class LazyConnectionContextSelector implements ContextSelector<EJBClientContext> {

    private final String hostUrl;
    private final CallbackHandler callbackHandler;

    private Endpoint endpoint;
    private Connection connection;

    private volatile EJBClientContext clientContext;


    public LazyConnectionContextSelector(final String hostUrl, final CallbackHandler callbackHandler) {
        this.hostUrl = hostUrl;
        this.callbackHandler = callbackHandler;
    }

    private synchronized void createConnection() {
        try {
            endpoint = Remoting.createEndpoint("endpoint", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));

            // open a connection
            final IoFuture<Connection> futureConnection = endpoint.connect(new URI(hostUrl), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE, Options.SASL_POLICY_NOPLAINTEXT, Boolean.FALSE), callbackHandler);
            connection = IoFutureHelper.get(futureConnection, 30L, TimeUnit.SECONDS);

            final EJBClientContext ejbClientContext = EJBClientContext.create();
            ejbClientContext.registerConnection(connection);

            this.clientContext = ejbClientContext;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public EJBClientContext getCurrent() {
        if (clientContext == null) {
            synchronized (this) {
                if (clientContext == null) {
                    createConnection();
                }
            }
        }
        return clientContext;
    }

    public synchronized void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            AppClientLogger.ROOT_LOGGER.exceptionClosingConnection(e);
        }
        try {
            if (endpoint != null) {
                endpoint.close();
            }
        } catch (IOException e) {
            AppClientLogger.ROOT_LOGGER.exceptionClosingConnection(e);
        }
    }
}
