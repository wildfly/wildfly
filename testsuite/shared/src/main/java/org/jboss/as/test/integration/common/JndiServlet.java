/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet which does JNDI lookup and returns looked up object class name in Http response. Lookup name is specified through
 * request parameter "name".
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@WebServlet(urlPatterns = {"/JndiServlet"})
public class JndiServlet extends HttpServlet {

    public static final String NOT_FOUND = "NOT_FOUND";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, NamingException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        String name = request.getParameter("name");
        if (name == null) {
            throw new ServletException("Lookup name not specified.");
        }
        InitialContext ctx = new InitialContext();
        Object obj = null;
        try {
            obj = ctx.lookup(name);
            out.print(obj.getClass().getName());
        } catch (NameNotFoundException nnfe) {
            out.print(NOT_FOUND);
        }
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NamingException ne) {
            throw new ServletException(ne);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NamingException ne) {
            throw new ServletException(ne);
        }
    }

    /*
     *  Simple servlet's client.
     */
    public static String lookup(String URL, String name) throws IOException {
        try {
            return HttpRequest.get(URL + "/JndiServlet?name=" + URLEncoder.encode(name, "UTF-8"), 5, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            return null;
        } catch (TimeoutException ex) {
            return null;
        }
    }
}
