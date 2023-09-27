/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.security.tg;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Testing servlet which enables transport guarantee security constraint.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class TransportGuaranteeServlet extends HttpServlet {

    private static final long serialVersionUID = 2L;

    public static final String servletContext = "/tg/srv";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("TransportGuaranteedGet");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("TransportGuaranteedPost");
    }

}

