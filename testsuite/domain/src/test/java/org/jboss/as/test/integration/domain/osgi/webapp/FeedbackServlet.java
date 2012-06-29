/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.domain.osgi.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A simple servlet.
 *
 * @author thomas.diesler@jboss.com
 */
@WebServlet(name = "FeedbackServlet", urlPatterns = { "/feedback" })
public class FeedbackServlet extends HttpServlet {

    // Provide logging
    static final Logger log = Logger.getLogger(FeedbackServlet.class);

    @Resource
    private BundleContext context;

    private BufferedReader service;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        final FeedbackServlet servlet = this;

        // Track {@link PaymentService} implementations
        ServiceTracker tracker = new ServiceTracker(context, BufferedReader.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference sref) {
                log.infof("Adding service: %s to %s", sref, servlet);
                service = (BufferedReader) super.addingService(sref);
                return service;
            }

            @Override
            public void removedService(ServiceReference sref, Object sinst) {
                super.removedService(sref, service);
                log.infof("Removing service: %s from %s", sref, servlet);
                service = null;
            }
        };
        tracker.open();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String message = process(req.getParameter("bnd"), req.getParameter("cmd"));
        PrintWriter out = res.getWriter();
        out.print(message);
        out.close();
    }

    private String process(String bnd, String cmd) throws IOException {
        String response = "Invalid parameters [bnd=" + bnd + ",cmd=" + cmd + "]";
        if (bnd != null) {
            Bundle[] bundles = getPackageAdmin().getBundles(bnd, null);
            if (bundles == null || bundles.length != 1) {
                return "Bundle not available: " + bnd;
            }
            Bundle bundle = bundles[0];
            try {
                if ("startlevel".equals(cmd)) {
                    response = bundle + ": startlevel==" + getStartLevel().getBundleStartLevel(bundle);
                } else if ("start".equals(cmd)) {
                    bundle.start();
                    response = bundle + ": state==" + bundle.getState();
                } else if ("stop".equals(cmd)) {
                    bundle.stop();
                    response = bundle + ": state==" + bundle.getState();
                } else {
                    if (service != null)
                        response = service.readLine() + ": " + cmd;
                    else
                        response = bundle + ": state==" + bundle.getState();
                }
            } catch (BundleException ex) {
                log.errorf(ex, "Failure executing command '%s' on: %s", cmd, bundle);
                response = bundle + ": " + ex;
            }
        }
        return response;
    }

    private StartLevel getStartLevel() {
        ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
        return (StartLevel) context.getService(sref);
    }

    private PackageAdmin getPackageAdmin() {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        return (PackageAdmin) context.getService(sref);
    }

    private static final long serialVersionUID = 1L;
}
