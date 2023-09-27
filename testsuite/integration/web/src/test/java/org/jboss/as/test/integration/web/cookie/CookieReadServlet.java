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
