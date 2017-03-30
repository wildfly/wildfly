/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.util.Headers;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * A service to setup a redirect for the web administration console.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ConsoleRedirectService implements Service<ConsoleRedirectService> {

    private static final String CONSOLE_PATH = "/console";
    private static final String NO_CONSOLE = "/noconsole.html";
    private static final String NO_REDIRECT = "/noredirect.html";

    private final InjectedValue<HttpManagement> httpManagementInjector = new InjectedValue<>();
    private final InjectedValue<Host> hostInjector = new InjectedValue<>();

    @Override
    public void start(final StartContext startContext) throws StartException {
        final Host host = hostInjector.getValue();
        UndertowLogger.ROOT_LOGGER.debugf("Starting console redirect for %s", host.getName());
        final HttpManagement httpManagement = httpManagementInjector.getOptionalValue();
        if (httpManagement != null) {
            if (httpManagement.hasConsole()) {
                host.registerHandler(CONSOLE_PATH, new ConsoleRedirectHandler(httpManagement));
            } else {
                host.registerHandler(CONSOLE_PATH, new RedirectHandler(NO_CONSOLE));
            }
        } else {
            host.registerHandler(CONSOLE_PATH, new RedirectHandler(NO_CONSOLE));
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        final Host host = hostInjector.getValue();
        UndertowLogger.ROOT_LOGGER.debugf("Stopping console redirect for %s", host.getName());
        host.unregisterHandler(CONSOLE_PATH);
    }

    @Override
    public ConsoleRedirectService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected InjectedValue<HttpManagement> getHttpManagementInjector() {
        return httpManagementInjector;
    }

    protected InjectedValue<Host> getHostInjector() {
        return hostInjector;
    }

    private static class ConsoleRedirectHandler implements HttpHandler {
        private static final int DEFAULT_PORT = 80;
        private static final String HTTP = "http";
        private static final String HTTPS = "https";
        private static final int SECURE_DEFAULT_PORT = 443;

        private final int port;
        private final int securePort;
        private final NetworkInterfaceBinding networkInterfaceBinding;
        private final NetworkInterfaceBinding secureNetworkInterfaceBinding;

        ConsoleRedirectHandler(final HttpManagement httpManagement) {
            port = httpManagement.getHttpPort();
            securePort = httpManagement.getHttpsPort();
            networkInterfaceBinding = httpManagement.getHttpNetworkInterfaceBinding();
            secureNetworkInterfaceBinding = httpManagement.getHttpsNetworkInterfaceBinding();
        }

        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {
            String location = NO_REDIRECT;
            // Both ports should likely never be less than 0, but should result in a NO_REDIRECT if they are
            if (port > -1 || securePort > -1) {
                try {
                    // Use secure port if available by default
                    if (securePort > -1) {
                        location = assembleURI(HTTPS, secureNetworkInterfaceBinding, securePort, SECURE_DEFAULT_PORT, exchange);
                    } else {
                        location = assembleURI(HTTP, networkInterfaceBinding, port, DEFAULT_PORT, exchange);
                    }
                } catch (URISyntaxException e) {
                    UndertowLogger.ROOT_LOGGER.invalidRedirectURI(e);
                }
            }
            // Use a new redirect each time as different clients could be requesting the console with different host names
            final RedirectHandler redirectHandler = new RedirectHandler(location);
            redirectHandler.handleRequest(exchange);
        }

        private String assembleURI(final String scheme, final NetworkInterfaceBinding interfaceBinding, final int port, final int defaultPort, final HttpServerExchange exchange)
                throws URISyntaxException {
            final int p = (port != defaultPort ? port : -1);
            final String hostname = getRedirectHostname(interfaceBinding.getAddress(), exchange);
            if (hostname == null) {
                return NO_REDIRECT;
            }
            return new URI(scheme, null, hostname, p, CONSOLE_PATH, null, null).toString();
        }

        private String getRedirectHostname(final InetAddress managementAddress, final HttpServerExchange exchange) {
            final InetAddress destinationAddress = exchange.getDestinationAddress().getAddress();
            // If hosts are equal use the host from the header
            if (managementAddress.equals(destinationAddress) || managementAddress.isAnyLocalAddress()) {
                String hostname = exchange.getRequestHeaders().getFirst(Headers.HOST);
                if (hostname != null) {
                    // Remove the port if it exists
                    final int portPos = hostname.indexOf(':');
                    if (portPos > 0) {
                        hostname = hostname.substring(0, portPos);
                    }
                    return hostname;
                }
            }

            // Host names don't match, use the IP address of the management host if both are loopback
            if (managementAddress.isLoopbackAddress() && destinationAddress.isLoopbackAddress()) {
                String hostname = managementAddress.getHostAddress();
                final int zonePos = hostname.indexOf('%');
                if (zonePos > 0) {
                    // Remove the zone identifier
                    hostname = hostname.substring(0, zonePos);
                }
                return hostname;
            }

            // Nothing matched, don't expose the management IP
            return null;
        }
    }
}
