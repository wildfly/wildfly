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
package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;

@WebServlet(urlPatterns = { MyServiceServlet.SERVLET_PATH })
public class MyServiceServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "service";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String SERVICE = "service";

    public static URI createURI(URL baseURL, ServiceName serviceName) throws URISyntaxException {
        return baseURL.toURI().resolve(new StringBuilder(SERVLET_NAME).append('?').append(SERVICE).append('=').append(serviceName.getCanonicalName()).toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String service = req.getParameter(SERVICE);
        if (service == null) {
            throw new ServletException(String.format("No %s specified", SERVICE));
        }
        Environment env = (Environment) CurrentServiceContainer.getServiceContainer().getService(ServiceName.parse(service)).getValue();
        if (env != null) {
            resp.setHeader("node", env.getNodeName());
        }
        resp.getWriter().write("Success");
    }
}
