/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { ValueServiceServlet.SERVLET_PATH })
public class ValueServiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SERVLET_NAME = "value";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String SERVICE = "service";
    public static final String PRIMARY_HEADER = "primary";
    private static final String EXPECTED = "expected";
//    private static final int RETRIES = 10;

    public static URI createURI(URL baseURL, ServiceName serviceName) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(serviceName).toString());
    }

    public static URI createURI(URL baseURL, ServiceName serviceName, boolean expected) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(serviceName).append('&').append(EXPECTED).append('=').append(expected).toString());
    }

    private static StringBuilder buildQuery(ServiceName serviceName) {
        return new StringBuilder(SERVLET_NAME).append('?').append(SERVICE).append('=').append(serviceName.getCanonicalName());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String serviceName = getRequiredParameter(request, SERVICE);
        this.log(String.format("Received request for %s", serviceName));
        @SuppressWarnings("unchecked")
        ServiceController<Boolean> service = (ServiceController<Boolean>) CurrentServiceContainer.getServiceContainer().getService(ServiceName.parse(serviceName));
        try {
            Boolean primary = service.awaitValue(5, TimeUnit.MINUTES);
            response.setHeader(PRIMARY_HEADER, primary.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            service.getServiceContainer().dumpServices();
            throw new ServletException(String.format("ServiceController %s did not provide a value within 5 minutes; " +
                    "mode is %s and state is %s", serviceName, service.getMode(), service.getState()), e);
        }
        response.getWriter().write("Success");
    }

    private static String getRequiredParameter(HttpServletRequest request, String name) throws ServletException {
        String value = request.getParameter(name);
        if (value == null) {
            throw new ServletException(String.format("No %s specified", name));
        }
        return value;
    }
}
