/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.formauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * A servlet that is secured by the web.xml descriptor. When accessed it simply
 * prints the getUserPrincipal that accessed the url.
 *
 * @author Scott.Stark@jboss.org
 */
public class SecureServlet extends HttpServlet {

    private static final long serialVersionUID = -5805093391064514424L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Principal user = request.getUserPrincipal();
        HttpSession session = request.getSession(false);
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>SecureServlet</title></head>");
        out.println("<h1>SecureServlet Accessed</h1>");
        out.println("<body>");
        out.println("You have accessed this servlet as user:" + user);
        if (session != null)
            out.println("<br>The session id is: " + session.getId());
        else
            out.println("<br>There is no session");
        out.println("</body></html>");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
