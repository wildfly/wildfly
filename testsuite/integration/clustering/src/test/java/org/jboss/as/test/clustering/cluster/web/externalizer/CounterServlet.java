/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web.externalizer;

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
@WebServlet(urlPatterns = { CounterServlet.SERVLET_PATH })
public class CounterServlet extends HttpServlet {
    private static final long serialVersionUID = -2155119413031863741L;

    private static final String SERVLET_NAME = "counter";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String COUNT_HEADER = "count";

    private static final String ATTRIBUTE = "counter";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        Counter counter = (Counter) session.getAttribute(ATTRIBUTE);
        int count = 0;
        if (counter == null) {
            counter = new Counter(count);
            session.setAttribute(ATTRIBUTE, counter);
        }
        resp.setIntHeader(COUNT_HEADER, counter.increment());
    }
}
