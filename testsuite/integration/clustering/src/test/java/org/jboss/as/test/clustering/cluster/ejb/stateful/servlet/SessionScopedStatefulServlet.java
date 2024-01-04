/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
