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

import org.jboss.as.server.mgmt.HttpManagementService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * A servlet that redirects traffic to /console to the http management interface
 *
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @version $Revision:$
 */
public class WelcomeContextConsoleServlet extends HttpServlet {

    private final InetSocketAddress location;
    private final InetSocketAddress secureLocation;
    private final boolean hasConsole;
    private final String noconsole = "noconsole.html";

    /**
     * Initialize the WelcomeContextConsoleServlet
     * @param httpMS - the HttpManagementService to obtain the console location from
     */
    WelcomeContextConsoleServlet(HttpManagementService httpMS) {
        if(httpMS != null) {
            this.location = httpMS.getBindAddress();
            this.secureLocation = httpMS.getSecureBindAddress();
            this.hasConsole = true;
        }
        else {
            this.location = null;
            this.secureLocation = null;
            this.hasConsole = false;
        }
    }

    /**
     * Redirect to the HttpManagementService location
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String target = noconsole;
        if(hasConsole) {
            if(req.isSecure()) {
                target = req.getScheme() + "://" + secureLocation.getHostName() + ":" + secureLocation.getPort() + req.getRequestURI();
            }
            else {
                target = req.getScheme() + "://" + location.getHostName() + ":" + location.getPort() + req.getRequestURI();
            }
        }
        resp.sendRedirect(target);
    }
}
