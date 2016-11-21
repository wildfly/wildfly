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

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
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

    private static final AuthenticationContextConfigurationClient AUTH_CONFIGURATION_CLIENT = doPrivileged(AuthenticationContextConfigurationClient.ACTION);

    private final String hostUrl;
    private final CallbackHandler callbackHandler;
    private final ClassLoader classLoader;


    private Endpoint endpoint;
    private Connection connection;

    private volatile EJBClientContext clientContext;


    public LazyConnectionContextSelector(final String hostUrl, final CallbackHandler callbackHandler, ClassLoader classLoader) {
        this.hostUrl = hostUrl;
        this.callbackHandler = callbackHandler;
        this.classLoader = classLoader;
    }

    private synchronized void createConnection() {
        try {
            final URI uri = new URI(hostUrl);
            AuthenticationContext captured = AuthenticationContext.captureCurrent();
            AuthenticationConfiguration mergedConfiguration = AUTH_CONFIGURATION_CLIENT.getAuthenticationConfiguration(uri, captured);
            if (callbackHandler != null) mergedConfiguration = mergedConfiguration.useCallbackHandler(callbackHandler);
            final AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, mergedConfiguration);

            // open a connection
            endpoint = Endpoint.builder().setEndpointName("endpoint").build();
            final IoFuture<Connection> futureConnection = endpoint.connect(uri, OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE, Options.SASL_POLICY_NOPLAINTEXT, Boolean.FALSE), context);
            connection = IoFutureHelper.get(futureConnection, 30L, TimeUnit.SECONDS);

            final EJBClientContext ejbClientContext = EJBClientContext.create(classLoader);
            ejbClientContext.registerConnection(connection);

            this.clientContext = ejbClientContext;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            if(clientContext == null) {
                //close it if we failed
                IoUtils.safeClose(connection, endpoint);
            }
        }
    }


    @Override
    public EJBClientContext getCurrent() {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<EJBClientContext>() {
            @Override
            public EJBClientContext run() {
                if (clientContext == null) {
                    synchronized (this) {
                        if (clientContext == null) {
                            createConnection();
                        }
                    }
                }
                return clientContext;
            }
        });
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
