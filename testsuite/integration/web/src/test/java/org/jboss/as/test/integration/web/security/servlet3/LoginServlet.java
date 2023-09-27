/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet3;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple servlet that tries to do a programmatic login
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login/"}, loadOnStartup = 1)
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 5442257117956926577L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = (String) req.getParameter("username");
        String password = (String) req.getParameter("password");
        req.login(username, password);
        Principal principal = req.getUserPrincipal();
        if (principal == null)
            throw new ServletException("getUserPrincipal returned null");
        String remoteUser = req.getRemoteUser();
        if (remoteUser == null)
            throw new ServletException("getRemoteUser returned null");
        String authType = req.getAuthType();
        if (authType == null || !(authType.equals("Programmatic") || "Programatic".equals(authType)))
            throw new ServletException(String.format("getAuthType returned wrong type '%s'", authType));
        if (!req.isUserInRole("gooduser")) {
            resp.sendError(403);
        }
        req.logout();
        principal = req.getUserPrincipal();
        if (principal != null)
            throw new ServletException("getUserPrincipal didn't return null after logout");
        remoteUser = req.getRemoteUser();
        if (remoteUser != null)
            throw new ServletException("getRemoteUser didn't return null after logout");
        authType = req.getAuthType();
        if (authType != null)
            throw new ServletException("getAuthType didn't return null after logout");
        if (req.isUserInRole("gooduser") || req.isUserInRole("superuser"))
            throw new ServletException("User shouldn't be in any roles after logout");
    }
}
