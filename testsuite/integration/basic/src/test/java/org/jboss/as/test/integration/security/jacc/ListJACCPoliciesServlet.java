/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.jacc;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Policy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple servlet that lists JACC policies.
 *
 * @author Josef Cacek
 */
@WebServlet(urlPatterns = {ListJACCPoliciesServlet.SERVLET_PATH})
public class ListJACCPoliciesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String ROOT_ELEMENT = "jacc-policies";
    public static final String SERVLET_PATH = "/listJACCPolicies";

    /**
     * Writes simple text response.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        writer.append("<" + ROOT_ELEMENT + ">\n");
        Policy policy = Policy.getPolicy();
        // Tests that use this class are disabled (see WFLY-4990 and WFLY-4991)
        // If they are ever re-enabled, this will fail until a test approach not reliant
        // on the old picketbox impl is available
        if (true) throw new IllegalStateException("Legacy security not supported");
//        if (policy instanceof DelegatingPolicy) {
//            writer.append(((DelegatingPolicy) policy).listContextPolicies() //
//                    //workarounds for https://issues.jboss.org/browse/SECURITY-663
//                    .replaceAll("Permission name=", "Permission' name=") //
//                    .replaceAll("RolePermssions", "RolePermissions")) //
//            ;
//        }
        writer.append("</" + ROOT_ELEMENT + ">\n");
        writer.close();
    }
}
