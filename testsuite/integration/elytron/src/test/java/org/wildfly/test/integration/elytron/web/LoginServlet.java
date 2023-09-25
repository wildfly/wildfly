/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.web;

import static org.wildfly.common.Assert.checkNotNullParam;
import java.io.IOException;
import java.io.Writer;
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple servlet to test programatic authentication.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 7207484039745630987L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = (String) req.getParameter("action");
        System.out.println("Action = " + action);

        if (action != null) {
            switch (action) {
                case "login":
                    String username = checkNotNullParam("username", (String) req.getParameter("username"));
                    String password = checkNotNullParam("password", (String) req.getParameter("password"));
                    System.out.println("username = " + username + " password = " + password);
                    req.login(username, password);
                    break;
                case "logout":
                    req.logout();
                    break;
            }
        }

        Writer writer = resp.getWriter();
        Principal principal = req.getUserPrincipal();
        writer.write(principal != null ? principal.getName() : "null");
    }

}
