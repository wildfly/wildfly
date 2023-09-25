/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;

@SuppressWarnings("serial")
@WebServlet("/TestUpgradeServlet")
public class TestUpgradeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/plain");

        if ("foo".equals(req.getHeader("Upgrade"))) {
            ComponentInterceptor.resetInterceptions();
            req.upgrade(TestHttpUpgradeHandler.class);
            resp.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
            resp.setHeader("Upgrade", "foo");
            resp.setHeader("Connection", "Upgrade");
        } else {
            resp.setStatus(500);
        }
    }

}
