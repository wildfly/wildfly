/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
