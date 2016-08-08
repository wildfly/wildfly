/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.formauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
