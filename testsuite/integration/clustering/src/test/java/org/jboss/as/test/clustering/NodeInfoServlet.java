/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet returning the serving node name.
 *
 * @author Ondrej Chaloupka
 */
@WebServlet(urlPatterns = { NodeInfoServlet.SERVLET_PATH })
public class NodeInfoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_NAME = "nodename";
    public static final String SERVLET_PATH = "/" + SERVLET_NAME;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(NodeNameGetter.getNodeName());
    }
}