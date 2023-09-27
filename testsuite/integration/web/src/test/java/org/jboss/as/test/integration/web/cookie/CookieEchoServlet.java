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
