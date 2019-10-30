/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

/**
 * Sends a version of the cookie back as a response body content.
 *
 * @author Jan Stourac
 */
@WebServlet(name = "CookieEchoServlet", urlPatterns = {"/CookieEchoServlet"})
public class CookieEchoServlet extends HttpServlet {

    private static final long serialVersionUID = -5891682551205336274L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        Cookie[] cookies = request.getCookies();

        PrintWriter out = response.getWriter();
        for (Cookie c : cookies) {
            out.print(c.getVersion());
        }
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        processRequest(request, response);
    }
}
