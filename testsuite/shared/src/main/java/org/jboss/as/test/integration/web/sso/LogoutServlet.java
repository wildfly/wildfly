/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * A servlet that logs out a user by invalidating any current session and then
 * redirects the user to the welcome page.
 *
 * @author Brian Stansberry
 */
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 2133162198049851268L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/index.html");
    }
}
