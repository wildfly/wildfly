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

package org.jboss.as.test.clustering.cluster.ejb.remote.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * A servlet which simply returns the node name it was executed on as a hearder value called "nodename".
 *
 * @author Richard Achmatowicz
 */
@WebServlet(urlPatterns = { WhichNodeServlet.SERVLET_PATH })
public class WhichNodeServlet extends HttpServlet {

    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "whichnode";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String HEADER_NODE_NAME = "nodename";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        URI result = createURI(baseURL.toURI());
        System.out.printf("Calling createURI: base name = %s, uri = %s\n", baseURL, result);
        return result;
    }

    public static URI createURI(URI baseURI) {
        return baseURI.resolve(SERVLET_NAME);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.getServletContext().log(String.format("[%s] %s?%s", request.getMethod(), request.getRequestURI(), request.getQueryString()));
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String nodeName = System.getProperty("jboss.node.name");
            response.setHeader(HEADER_NODE_NAME, nodeName);
        } catch (Exception ignore) {
        }
        response.getWriter().write("Success");
    }
}
