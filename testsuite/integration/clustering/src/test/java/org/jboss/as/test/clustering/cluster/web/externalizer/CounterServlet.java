/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.externalizer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
