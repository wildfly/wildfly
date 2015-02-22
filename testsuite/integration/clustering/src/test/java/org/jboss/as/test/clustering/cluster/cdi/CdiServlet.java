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
package org.jboss.as.test.clustering.cluster.cdi;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Tomas Remes
 */
@WebServlet(urlPatterns = CdiServlet.SERVLET_PATH)
public class CdiServlet extends HttpServlet {
    private static final long serialVersionUID = 5167789049451718160L;

    public static final String SERVLET_NAME = "cdi";
    public static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String SESSION_ID_HEADER = "sessionId";
    public static final String COUNT = "count";

    @Inject
    private Incrementor incrementor;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        resp.addHeader(SESSION_ID_HEADER, session.getId());
        resp.setIntHeader(COUNT, this.incrementor.increment());
        resp.getWriter().write("Success");
    }

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }
}
