/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.classpath;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.web.classpath.util.Debug;

/**
 * A servlet that loads classes requested via the class request param.
 *
 * @author Scott.Scott@jboss.org
 */
public class ClasspathServlet2 extends HttpServlet {
    private static final long serialVersionUID = 1;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String className = request.getParameter("class");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>" + getServletName() + "</title></head>");
        out.println("<body><h1>Class Info</h1>");
        try {
            Class<?> clazz = Class.forName(className);
            ProtectionDomain pd = clazz.getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            response.addHeader("X-CodeSource", cs.getLocation().toString());
            out.println("<pre>\n");
            StringBuffer results = new StringBuffer();
            Debug.displayClassInfo(clazz, results);
            out.println(results.toString());
            out.println("</pre>");
        } catch (Exception e) {
            out.println("Failed to load " + className);
            out.println("<pre>\n");
            e.printStackTrace(out);
            out.println("</pre>");
            response.addHeader("X-Exception", e.getMessage());
        }

        out.println("</html>");
        out.close();
    }

}
