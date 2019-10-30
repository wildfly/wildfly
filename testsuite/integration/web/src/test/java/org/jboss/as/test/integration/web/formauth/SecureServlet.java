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
import javax.servlet.http.HttpSession;
import javax.security.auth.Subject;

import org.jboss.security.SecurityContextAssociation;

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
        String validateSubject = request.getParameter("validateSubject");
        if (validateSubject != null && Boolean.valueOf(validateSubject).booleanValue()) {
            // Assert that there is a valid SecurityAssociation Subject
            Subject subject = SecurityContextAssociation.getSubject();
            if (subject == null)
                throw new ServletException("No valid subject found, user=" + user);
        }
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
