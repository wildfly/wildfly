/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility servlet that waits until the specified cluster establishes a specific cluster membership.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { ViewChangeListenerServlet.SERVLET_PATH })
public class ViewChangeListenerServlet extends HttpServlet {
    private static final long serialVersionUID = -4382952409558738642L;
    private static final String SERVLET_NAME = "membership";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String CLUSTER = "cluster";
    public static final String MEMBERS = "members";

    @EJB
    private ViewChangeListener listener;

    public static URI createURI(URL baseURL, String cluster, String... members) throws URISyntaxException {
        StringBuilder builder = new StringBuilder(baseURL.toURI().resolve(SERVLET_NAME).toString());
        builder.append('?').append(CLUSTER).append('=').append(cluster);
        for (String member: members) {
            builder.append('&').append(MEMBERS).append('=').append(member);
        }
        return URI.create(builder.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String cluster = req.getParameter(CLUSTER);
        if (cluster == null) {
            throw new ServletException(String.format("No '%s' parameter specified", CLUSTER));
        }
        String[] members = req.getParameterValues(MEMBERS);
        try {
            this.listener.establishView(cluster, members);
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }
    }
}
