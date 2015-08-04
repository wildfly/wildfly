/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.security.context;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.security.DeclareRoles;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for invocation EjbSecurityDomainAsServlet or EjbOwnSecurityDomain EJB.
 *
 * @author olukas
 */
@WebServlet(urlPatterns = {ReuseAuthenticatedSubjectServlet.SERVLET_PATH})
@DeclareRoles({ReuseAuthenticatedSubjectServlet.AUTHORIZED_ROLE})
@ServletSecurity(
        @HttpConstraint(rolesAllowed = {ReuseAuthenticatedSubjectServlet.AUTHORIZED_ROLE}))
public class ReuseAuthenticatedSubjectServlet extends HttpServlet {

    public static final String SERVLET_PATH = "/ReuseAuthenticatedSubjectServlet";

    public static final String SAME_SECURITY_DOMAIN_PARAM = "sameSecuriryDomain";

    public static final String AUTHORIZED_ROLE = "admin";

    @EJB
    EjbSecurityDomainAsServlet ejbSecurityDomainAsServlet;

    @EJB
    EjbOwnSecurityDomain ejbOwnSecurityDomain;

    /**
     * Invoke EJB and write its output. If sameSecuriryDomain parameter is set to true, then EjbSecurityDomainAsServlet is used.
     * Otherwise EjbOwnSecurityDomain is used.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        if (Boolean.parseBoolean(req.getParameter(SAME_SECURITY_DOMAIN_PARAM))) {
            writer.write(ejbSecurityDomainAsServlet.sayHello());
        } else {
            writer.write(ejbOwnSecurityDomain.sayHello());
        }
        writer.close();
    }
}
