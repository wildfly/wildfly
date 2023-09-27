/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
