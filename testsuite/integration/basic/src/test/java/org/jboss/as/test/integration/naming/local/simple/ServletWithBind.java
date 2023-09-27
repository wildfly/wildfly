/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming.local.simple;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ServletWithBind", urlPatterns = {"/simple"})
public class ServletWithBind extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String msg = req.getParameter("op");
        if ("bind".equals(msg)) {
            try {
                final Context context = new InitialContext();
                context.bind("java:jboss/web-test", "Test");
                context.bind("java:/web-test", "Test");
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        }
    }
}
