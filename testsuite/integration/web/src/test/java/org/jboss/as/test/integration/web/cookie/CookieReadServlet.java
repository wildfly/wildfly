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
package org.jboss.as.test.integration.web.cookie;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

@WebServlet(name = "CookieReadServlet", urlPatterns = { "/CookieReadServlet" })
public class CookieReadServlet extends HttpServlet {

    private static final long serialVersionUID = 2621436577320182272L;

    Logger log = Logger.getLogger(CookieReadServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Cookie Read Servlet</title></head><body><pre>");
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.trace("cookie is null");
            setCookies(request, response);
            out.println("Server set cookies correctly");
        } else {
            for (int i = 0; i < cookies.length; i++) {
                Cookie c = cookies[i];
                out.println("Cookie" + i + "Name " + c.getName() + " value=" + c.getValue());
                if (c.getName().equals("hasSpace") && c.getValue().indexOf("\"") != -1) {
                    log.debug("Cookie name: " + c.getName() + " cookie value: " + c.getValue());
                    throw new ServletException("cookie with space not retrieved correctly");
                } else if (c.getName().equals("hasComma") && c.getValue().indexOf("\"") != -1) {
                    log.debug("Cookie name: " + c.getName() + " cookie value: " + c.getValue());
                    throw new ServletException("cookie with comma not retrieved correctly");
                }
            }
            out.println("Server read cookie correctly");
        }
        out.println("</pre></body></html>");
        out.close();
    }

    public void setCookies(HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(new Cookie("hasSpace", "has space"));
        response.addCookie(new Cookie("hasComma", "has,comma"));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
