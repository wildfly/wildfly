/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.mgmt.domain.HttpManagement;

/**
 * A servlet that redirects traffic to /console to the http management interface
 *
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class WelcomeContextConsoleServlet extends HttpServlet {

    private static final String CONSOLE_PATH = "/console";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final int DEFAULT_PORT = 80;
    private static final int SECURE_DEFAULT_PORT = 443;

    private final boolean hasConsole;

    private final int consolePort;
    private final int consoleSecurePort;
    private final NetworkInterfaceBinding consoleNetworkInterface;
    private final NetworkInterfaceBinding secureConsoleNetworkInterface;

    private final String noconsole = "noconsole.html";
    private final String noredirect = "noredirect.html";

    /**
     * Initialize the WelcomeContextConsoleServlet
     *
     * @param httpManagement - the HttpManagement to obtain the console location from
     */
    WelcomeContextConsoleServlet(HttpManagement httpManagement) {
        if (httpManagement != null) {
            consolePort = httpManagement.getHttpPort();
            consoleSecurePort = httpManagement.getHttpsPort();
            consoleNetworkInterface = httpManagement.getHttpNetworkInterfaceBinding();
            secureConsoleNetworkInterface = httpManagement.getHttpsNetworkInterfaceBinding();
            // If there is no port there is no console.
            hasConsole = consolePort > -1 || consoleSecurePort > -1;
        } else {
            hasConsole = false;
            consolePort = -1;
            consoleSecurePort = -1;
            consoleNetworkInterface = null;
            secureConsoleNetworkInterface = null;
        }
    }

    /**
     * Redirect to the HttpManagementService location
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String target = noconsole;
        if (hasConsole) {
            InetAddress inboundAddress = InetAddress.getByName(req.getLocalAddr());

            boolean secureRedirect = secureRedirect(req.isSecure());
            NetworkInterfaceBinding interfaceBinding = secureRedirect ? secureConsoleNetworkInterface : consoleNetworkInterface;
            String redirectHost = getRedirectHost(interfaceBinding, inboundAddress, req.getServerName());

            if (redirectHost != null) {
                if (secureRedirect) {
                    target = assembleURI(HTTPS, redirectHost, consoleSecurePort, SECURE_DEFAULT_PORT, CONSOLE_PATH);
                } else {
                    target = assembleURI(HTTP, redirectHost, consolePort, DEFAULT_PORT, CONSOLE_PATH);
                }
            } else {
                target = noredirect;
            }
        }
        resp.sendRedirect(target);
    }

    private String assembleURI(final String scheme, final String host, final int port, final int defaultPort, final String uri)
            throws IOException {
        URI redirectUri;
        try {
            if (port != defaultPort) {
                redirectUri = new URI(scheme, null, host, port, uri, null, null);
            } else {
                redirectUri = new URI(scheme, null, host, -1, uri, null, null);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return redirectUri.toString();
    }

    /**
     * Should the inbound request be redirected to the secure port of the HttpManagementService.
     *
     * @param inboundSecure Was the inbound request a secure request.
     * @return true if a secure redirect should be used.
     */
    private boolean secureRedirect(final boolean inboundSecure) {
        // To reach this point we know that at least one port is enabled.
        if (inboundSecure) {
            // The redirect can only be secure if there is a secure port on the HttpManagementService.
            return consoleSecurePort > -1;
        } else {
            // If the inbound request was not secure but the HttpManagementService is only listening on a secure
            // port then we have to redirect to the secure port.
            return consolePort > -1 == false;
        }
    }

    /**
     * Takes the chosen NetworkInterfaceBinding, inbound address and host from the HTTP header and decides what address to
     * redirect to.
     *
     * @param chosenBinding - The NetworkInterfaceBinding of the http interface.
     * @param inboundAddress - The local address the request was recieved on.
     * @param headerHost - The host from the HTTP header.
     * @return - The host name to redirect to or null of no valid host could be identified.
     */
    private String getRedirectHost(final NetworkInterfaceBinding chosenBinding, final InetAddress inboundAddress,
            final String headerHost) {
        InetAddress managementAddress = chosenBinding.getAddress();
        if (managementAddress.equals(inboundAddress) || managementAddress.isAnyLocalAddress()) {
            // Here we know that either there is a direct match between the address being listened on
            // by the management interface OR the management interface is listening on all addresses.
            //
            // In this case the host references in the header should be useable.
            return headerHost;
        }

        if (managementAddress.isLoopbackAddress() && inboundAddress.isLoopbackAddress()) {
            // If they are both loopback addresses the comparison above failed to match them
            // so they must be different loopback bindings e.g. 127.0.0.1 and ::1 as this is a
            // loopback to loopback redirection we will allow the other address to be passed to
            // the remote client.

            return managementAddress.getHostAddress();
        }

        // To reach this point there was no correlation identified between the inbound address and the address the
        // management interface is listening on - we do not know if this client should even know about the management
        // console so do not redirect.
        //
        // Once network configurations become complex administrators should be connecting directly to the management
        // console and not relying on a redirect from the connector serving up their deployed web applications.

        return null;
    }

}
