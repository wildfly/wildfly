/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.stateful.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.ScopedIncrementor;

/**
 * Test servlet that uses a injected @SessionScoped SFSB.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SessionScopedStatefulServlet.SERVLET_PATH })
public class SessionScopedStatefulServlet extends AbstractStatefulServlet {
    private static final long serialVersionUID = 6261258621320385992L;
    private static final String SERVLET_NAME = "scoped-count";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Inject
    private ScopedIncrementor bean;

    @Override
    public Incrementor apply(HttpServletRequest request) {
        return this.bean;
    }

    @Override
    public void accept(HttpSession session, Incrementor incrementor) {
        // SFSB is already scoped to the HttpSession.
    }
}
