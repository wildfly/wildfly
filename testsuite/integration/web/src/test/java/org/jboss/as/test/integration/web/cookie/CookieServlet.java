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

/**
 * A servlet that is used to test different way of setting and retrieving cookies.
 *
 * @author prabhat.jha@jboss.com
 */
@WebServlet(name = "CookieServlet", urlPatterns = { "/CookieServlet" })
public class CookieServlet extends HttpServlet {

    private static final long serialVersionUID = -5891682551205336273L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Cookie Servlet</title></head><body><pre>");
        setRFC2109cookies(request, response);
        out.println("sever set some cookies. verify on the client that you can see them");
        out.println("</pre></body></html>");
        out.close();
    }

    private void setRFC2109cookies(HttpServletRequest request, HttpServletResponse response) {
        // A very simple cookie
        Cookie cookie = new Cookie("simpleCookie", "jboss");
        response.addCookie(cookie);

        // A cookie with space in the value. As per ASPATCH-70, there has been some issue with this.
        cookie = new Cookie("withSpace", "jboss rocks");
        response.addCookie(cookie);

        // cookie with comment
        // TODO read servlet 2.5 spec and rfc2109, then re-fix it
        /*
         * Servlet 2.5 Cookie.java disable comment attribute
         * cookie = new Cookie("comment", "commented cookie");
         * cookie.setComment("This is a comment");
         * response.addCookie(cookie);
         */

        // cookie with expiry time. This cookie must not be set on client side
        cookie = new Cookie("expired", "expired cookie");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        cookie = new Cookie("withComma", "little,comma");
        response.addCookie(cookie);

        cookie = new Cookie("expireIn10Sec", "will expire in 10 seconds");
        cookie.setMaxAge(10);
        response.addCookie(cookie);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
