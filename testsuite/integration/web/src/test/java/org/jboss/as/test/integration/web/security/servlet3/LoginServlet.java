/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.servlet3;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        if (authType == null || !authType.equals("Programatic"))
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
