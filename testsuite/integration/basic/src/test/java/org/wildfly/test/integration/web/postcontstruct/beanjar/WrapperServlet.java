/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.web.postcontstruct.beanjar;

import java.io.IOException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class WrapperServlet extends HttpServlet {
    private HttpServlet delegate;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        delegate = config.getServletContext().createServlet(SimpleServlet.class);
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        delegate.service(req, resp);
    }
}
