/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ws.serviceref;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

/**
 *
 * @author Matus Abaffy
 */
@WebServlet(name = "ServletLoadOnStartup", urlPatterns = { "/servletLOS" }, loadOnStartup = 1)
public class ServletLoadOnStartup extends HttpServlet {

    private static final long serialVersionUID = -5664912198621349534L;

    @WebServiceRef(value = EndpointService.class)
    private EndpointInterface endpoint1;

    private EndpointInterface endpoint2;

    @WebServiceRef(value = EndpointService.class)
    public void setEndpoint2(EndpointInterface endpoint) {
        endpoint2 = endpoint;
    }

    private String echo1(final String string) {
        if (null == endpoint1) {
            throw new IllegalStateException("@WebServiceRef field injection failed for 'endpoint1'");
        }
        return endpoint1.echo(string);
    }

    private String echo2(final String string) {
        if (null == endpoint2) {
            throw new IllegalStateException("@WebServiceRef setter injection failed for 'endpoint2'");
        }
        return endpoint2.echo(string);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String echo = req.getParameter("echo");
        String type = req.getParameter("type");
        Writer writer = resp.getWriter();

        if (type.equals("field")) {
            writer.write(echo1(echo));
        } else if (type.equals("setter")) {
            writer.write(echo2(echo));
        } else {
            resp.setStatus(404);
        }
    }
}
