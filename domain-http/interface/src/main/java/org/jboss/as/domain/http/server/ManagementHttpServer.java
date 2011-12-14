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
package org.jboss.as.domain.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.domain.http.server.security.BasicAuthenticator;
import org.jboss.as.domain.http.server.security.ClientCertAuthenticator;
import org.jboss.as.domain.http.server.security.DigestAuthenticator;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.com.sun.net.httpserver.HttpsConfigurator;
import org.jboss.com.sun.net.httpserver.HttpsParameters;
import org.jboss.com.sun.net.httpserver.HttpsServer;
import org.jboss.modules.ModuleLoadException;
import org.jboss.sasl.callback.DigestHashCallback;

/**
 * The general HTTP server for handling management API requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ManagementHttpServer {

    private final HttpServer httpServer;

    private final HttpServer secureHttpServer;

    private SecurityRealm securityRealm;

    private List<ManagementHttpHandler> handlers = new LinkedList<ManagementHttpHandler>();

    private ManagementHttpServer(HttpServer httpServer, HttpServer secureHttpServer, SecurityRealm securityRealm) {
        this.httpServer = httpServer;
        this.secureHttpServer = secureHttpServer;
        this.securityRealm = securityRealm;
    }

    void addHandler(ManagementHttpHandler handler) {
        handlers.add(handler);
    }

    public void start() {
        start(httpServer);
        start(secureHttpServer);
    }

    private void start(HttpServer httpServer) {
        if (httpServer == null)
            return;

        for (ManagementHttpHandler current : handlers) {
            current.start(httpServer, securityRealm);
        }
        httpServer.start();
    }

    public void stop() {
        stop(httpServer);
        stop(secureHttpServer);
    }

    private void stop(HttpServer httpServer) {
        if (httpServer == null)
            return;

        httpServer.stop(0);
        for (ManagementHttpHandler current : handlers) {
            current.stop(httpServer);
        }
    }

    public static ManagementHttpServer create(InetSocketAddress bindAddress, InetSocketAddress secureBindAddress, int backlog, ModelControllerClient modelControllerClient, Executor executor, SecurityRealm securityRealm, ConsoleMode consoleMode)
            throws IOException {
        Map<String, String> configuration = new HashMap<String, String>(1);
        configuration.put("sun.net.httpserver.maxReqTime", "15"); // HTTP Server to close connections if initial request not received within 15 seconds.

        Authenticator auth = null;
        final CertAuth certAuthMode;

        if (securityRealm != null) {
            DomainCallbackHandler callbackHandler = securityRealm.getCallbackHandler();
            Class[] supportedCallbacks = callbackHandler.getSupportedCallbacks();
            if (DigestAuthenticator.requiredCallbacksSupported(supportedCallbacks)) {
                auth = new DigestAuthenticator(callbackHandler, securityRealm.getName(), contains(DigestHashCallback.class,
                        supportedCallbacks));
            } else if (BasicAuthenticator.requiredCallbacksSupported(supportedCallbacks)) {
                auth = new BasicAuthenticator(callbackHandler, securityRealm.getName());
            }

            if (securityRealm.hasTrustStore()) {
                // For this to return true we know we have a trust store to use to verify client certificates.
                if (auth == null) {
                    certAuthMode = CertAuth.NEED;
                    auth = new ClientCertAuthenticator(securityRealm.getName());
                } else {
                    // We have the possibility to use Client Cert but also Username/Password authentication so don't
                    // need to force clients into presenting a Cert.
                    certAuthMode = CertAuth.WANT;
                }
            } else {
                certAuthMode = CertAuth.NONE;
            }
        } else {
            certAuthMode = CertAuth.NONE;
        }

        HttpServer httpServer = null;
        if (bindAddress != null) {
            httpServer = HttpServer.create(bindAddress, backlog, configuration);
            httpServer.setExecutor(executor);
        }

        HttpsServer secureHttpServer = null;
        if (secureBindAddress != null) {
            secureHttpServer = HttpsServer.create(secureBindAddress, backlog, configuration);
            final SSLContext context = securityRealm.getSSLContext();
            secureHttpServer.setHttpsConfigurator(new HttpsConfigurator(context) {

                @Override
                public void configure(HttpsParameters params) {
                    SSLParameters sslparams = context.getDefaultSSLParameters();

                    switch (certAuthMode) {
                        case NEED:
                            sslparams.setNeedClientAuth(true);
                            break;
                        case WANT:
                            sslparams.setWantClientAuth(true);
                            break;
                    }

                    params.setSSLParameters(sslparams);

                }
            });

            secureHttpServer.setExecutor(executor);
        }

        ManagementHttpServer managementHttpServer = new ManagementHttpServer(httpServer, secureHttpServer, securityRealm);
        ResourceHandler consoleHandler;
        try {
            consoleHandler = consoleMode.createConsoleHandler();
        } catch (ModuleLoadException e) {
            throw new IOException("Unable to load resource handler", e);
        }
        managementHttpServer.addHandler(new RootHandler(consoleHandler));
        managementHttpServer.addHandler(new DomainApiHandler(modelControllerClient, auth));
        if (consoleHandler != null) {
            managementHttpServer.addHandler(consoleHandler);
        }

        try {
            managementHttpServer.addHandler(new ErrorHandler());
        } catch (ModuleLoadException e) {
            throw new IOException("Unable to load resource handler", e);
        }

        return managementHttpServer;
    }

    // TODO - This still needs cleaning up to use collections so we can remove the array iteration.
    private static boolean contains(Class clazz, Class[] classes) {
        for (Class current : classes) {
            if (current.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private enum CertAuth {
        NONE, WANT, NEED
    }

}
