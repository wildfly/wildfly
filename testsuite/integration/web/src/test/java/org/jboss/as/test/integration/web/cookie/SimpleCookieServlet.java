/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.cookie;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet that is used to test different way of setting and retrieving cookies.
 *
 * @author prabhat.jha@jboss.com
 * @author Jan Stourac
 */
@WebServlet(name = "SimpleCookieServlet", urlPatterns = {"/SimpleCookieServlet"})
public class SimpleCookieServlet extends HttpServlet {

    private static final long serialVersionUID = -5891682551205336273L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Cookie Servlet</title></head><body><pre>");
        setSimplecookie(request, response);
        out.println("sever set some cookies. verify on the client that you can see them");
        out.println("</pre></body></html>");
        out.close();
    }

    private void setSimplecookie(HttpServletRequest request, HttpServletResponse response) {
        // A very simple cookie
        Cookie cookie = new Cookie("simpleCookie", "jboss");
        response.addCookie(cookie);
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
