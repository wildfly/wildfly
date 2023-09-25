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

/**
 * A secured servlet which is the target of a post from an unsecured servlet.
 * This validates that the post data is not lost when the original post is
 * redirected to the form auth login page.
 *
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
public class SecuredPostServlet extends HttpServlet {

    private static final long serialVersionUID = -1996243573114825441L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        Principal user = request.getUserPrincipal();
        String path = request.getPathInfo();
        // Validate that there is an authenticated user
        if (user == null)
            throw new ServletException(path + " not secured");
        // Validate that the original post data was not lost
        String value = request.getParameter("checkParam");
        if (value == null || value.equals("123456") == false)
            throw new ServletException("Did not find checkParam=123456");

        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        out.println("<html>");
        out.println("<head><title>" + path + "</title></head><body>");
        out.println("<h1>" + path + " Accessed</h1>");
        out.println("You have accessed this servlet as user: " + user);
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
