/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.security.bufferFreed;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.EndpointBuilder;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import java.net.URI;

import static org.jboss.as.test.shared.TestSuiteEnvironment.getSystemProperty;

/**
 * Class provides remote connections for BufferFreedTestCase in a cycle with the given number of iterations.
 *
 * @author Daniel Cihak
 */
public class TestRemotingConnection implements Runnable {

    private static final Logger log = Logger.getLogger(TestRemotingConnection.class.getSimpleName());

    private static final String PROTOCOL = "https-remoting";
    private static final int PORT = Integer.parseInt(getSystemProperty("jboss.http.port", "8443"));

    private String name;
    private Config config;
    private Throwable exception;

    public TestRemotingConnection(String name, Config config) {
        this.name = name;
        this.config = config;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public void run() {
        Connection connection = null;
        Endpoint endpoint = null;
        for (int i = 0; i < this.config.getIterations(); i++) {
            try {
                if (!this.config.isRunning()) {
                    break;
                }
                URI uri = new URI(this.getUrl());

                final OptionMap.Builder connectionOptions = OptionMap.builder();
                connectionOptions.set(Options.SSL_ENABLED, Boolean.TRUE);
                connectionOptions.set(Options.SSL_STARTTLS, Boolean.TRUE);
                connectionOptions.set(Options.SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);
                connectionOptions.set(Options.SASL_POLICY_NOANONYMOUS, Boolean.TRUE);
                connectionOptions.setSequence(Options.SASL_DISALLOWED_MECHANISMS, "JBOSS-LOCAL-USER");

                EndpointBuilder builder = Endpoint.builder();
                builder.setEndpointName(name + "endpoint-" + i);
                builder.addConnection(uri);
                builder.buildXnioWorker(Xnio.getInstance());
                endpoint = builder.build();

                AuthenticationConfiguration authenticationConfiguration = AuthenticationConfiguration.empty();
                authenticationConfiguration.useName(this.config.getUsername());
                authenticationConfiguration.usePassword(this.config.getPassword().toCharArray());
                AuthenticationContext authenticationContext = AuthenticationContext.empty().with(MatchRule.ALL, AuthenticationConfiguration.empty().useName(this.config.getUsername()).usePassword(this.config.getPassword().toCharArray()));

                final IoFuture<Connection> futureConnection = endpoint.connect(uri, connectionOptions.getMap(), authenticationContext);
                connection = futureConnection.get();

            } catch (Throwable t) {
                log.error(t);
                this.exception = t;
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                if (endpoint != null) {
                    try {
                        endpoint.close();
                        endpoint = null;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String getUrl() {
        return String.format("%s://%s:%d", PROTOCOL, TestSuiteEnvironment.getServerAddress(), PORT);
    }
}
