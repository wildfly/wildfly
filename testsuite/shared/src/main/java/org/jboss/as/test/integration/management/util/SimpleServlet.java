/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.util;

import java.io.IOException;
import java.io.PrintWriter;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@WebServlet(urlPatterns={"/SimpleServlet"})
public class SimpleServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        final String envEntry = request.getParameter("env-entry");
        if(envEntry == null) {
            out.println("<html>");
            out.println("<head><title>SimpleServlet</title></head>");
            out.println("<body>Done</body>");
            out.println("</html>");
        } else {
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();
                out.println(ctx.lookup("java:comp//env/" + envEntry));
                ctx.close();
            } catch (NamingException e) {
                e.printStackTrace(out);
            } finally {
                if(ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                    }
                }
            }
        }
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
