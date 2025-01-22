/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet that used to accesses Jakarta Enterprise Beans bean but for the last 11
 * years has been pretending.
 *
 * @author Scott.Stark@jboss.org
 */
public class NotAnEJBServlet extends HttpServlet {

    private static final long serialVersionUID = 2070931818661985879L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head><title>NotAnEJBServlet</title></head>");
            out.println("<body>Tests passed<br>Time:" + new Date().toString() + "</body>");
            out.println("</html>");
        }
    }
}
