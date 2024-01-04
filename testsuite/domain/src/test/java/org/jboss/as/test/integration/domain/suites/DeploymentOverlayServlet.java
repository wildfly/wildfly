/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.suites;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/servlet")
public class DeploymentOverlayServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final String result = (String) new InitialContext().lookup("java:comp//env/simpleString");
            resp.getWriter().write(result);
            resp.getWriter().close();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
