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

package org.jboss.as.test.clustering.single.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SimpleServlet.SERVLET_PATH })
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "simple";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String REQUEST_DURATION_PARAM = "requestduration";
    public static final String HEADER_SERIALIZED = "serialized";
    public static final String VALUE_HEADER = "value";
    public static final String SESSION_ID_HEADER = "sessionId";
    public static final String ATTRIBUTE = "test";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL.toURI());
    }

    public static URI createURI(URI baseURI) {
        return baseURI.resolve(SERVLET_NAME);
    }

    public static URI createURI(URL baseURL, int requestDuration) throws URISyntaxException {
        return createURI(baseURL.toURI(), requestDuration);
    }

    public static URI createURI(URI baseURI, int requestDuration) {
        return baseURI.resolve(SERVLET_NAME + '?' + REQUEST_DURATION_PARAM + '=' + requestDuration);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        resp.addHeader(SESSION_ID_HEADER, session.getId());
        Mutable custom = (Mutable) session.getAttribute(ATTRIBUTE);
        if (custom == null) {
            custom = new Mutable(1);
            session.setAttribute(ATTRIBUTE, custom);
        } else {
            custom.increment();
        }
        resp.setIntHeader(VALUE_HEADER, custom.getValue());
        resp.setHeader(HEADER_SERIALIZED, Boolean.toString(custom.wasSerialized()));

        this.getServletContext().log(req.getRequestURI() + ", value = " + custom.getValue());

        // Long running request?
        if (req.getParameter(REQUEST_DURATION_PARAM) != null) {
            int duration = Integer.valueOf(req.getParameter(REQUEST_DURATION_PARAM));
            try {
                Thread.sleep(duration);
            } catch (InterruptedException ex) {
            }
        }

        resp.getWriter().write("Success");
    }
}
