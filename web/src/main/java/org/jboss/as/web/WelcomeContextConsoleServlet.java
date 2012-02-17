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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Enumeration;

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
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
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
            // First check that the address used to contact the JBoss Web connector is accessible over the network interfaces
            // assigned for the HttpManagementService
            boolean secureRedirect = secureRedirect(req.isSecure());
            if (isAccessible(inboundAddress, secureRedirect)) {

                String host = req.getServerName();
                if (secureRedirect) {
                    target = assembleURL(HTTPS, host, consoleSecurePort, SECURE_DEFAULT_PORT, CONSOLE_PATH);
                } else {
                    target = assembleURL(HTTP, host, consolePort, DEFAULT_PORT, CONSOLE_PATH);
                }
            } else {
                target = noredirect;
            }
        }
        resp.sendRedirect(target);
    }

    private String assembleURL(final String scheme, final String host, final int port, final int defaultPort, final String uri) {
        StringBuilder targetBuilder = new StringBuilder(scheme);
        targetBuilder.append(host);
        if (port != defaultPort) {
            targetBuilder.append(":");
            targetBuilder.append(port);
        }
        targetBuilder.append(uri);

        return targetBuilder.toString();
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

    private boolean isAccessible(final InetAddress inboundAddress, final boolean secure) {
        final NetworkInterfaceBinding interfaceBinding = secure ? secureConsoleNetworkInterface : consoleNetworkInterface;
        Collection<NetworkInterface> nics = interfaceBinding.getNetworkInterfaces();

        for (NetworkInterface current : nics) {
            boolean matched = matches(current, inboundAddress);
            if (matched) {
                return true;
            }

        }
        return false;
    }

    private boolean matches(NetworkInterface nic, InetAddress match) {
        Enumeration<InetAddress> addresses = nic.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (match.equals(address)) {
                return true;
            }
        }

        Enumeration<NetworkInterface> nics = nic.getSubInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface next = nics.nextElement();
            boolean matches = matches(next, match);
            if (matches) {
                return true;
            }
        }

        return false;
    }

}
