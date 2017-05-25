/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.servlet3;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple servlet that tries to do a programmatic login
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a> TODO
 */
public class RoleNamesAnnotationsServlet {
    /**
     * A simple servlet that restricts access only to users registered among war security-roles.
     *
     * @author Jan Stourac
     */
    @WebServlet(name = "RoleNamesAnnotationsSecuredServlet", urlPatterns = {ServletSecurityRoleNamesCommon
            .SECURED_INDEX}, loadOnStartup = 1)
    @ServletSecurity(@HttpConstraint(rolesAllowed = {"*"}))
    public static class RoleNamesAnnotationsSecuredServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write(ServletSecurityRoleNamesCommon.SECURED_INDEX_CONTENT);
        }
    }

    /**
     * A simple servlet that restricts access only to any logged-in users.
     *
     * @author Jan Stourac
     */
    @WebServlet(name = "RoleNamesAnnotationsWeaklySecuredServlet", urlPatterns = {ServletSecurityRoleNamesCommon
            .WEAKLY_SECURED_INDEX}, loadOnStartup = 1)
    @ServletSecurity(@HttpConstraint(rolesAllowed = {"**"}))
    public static class RoleNamesAnnotationsWeaklySecuredServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write(ServletSecurityRoleNamesCommon.WEAKLY_SECURED_INDEX_CONTENT);
        }
    }

    /**
     * A simple servlet that restricts access to any users.
     *
     * @author Jan Stourac
     */
    @WebServlet(name = "RoleNamesAnnotationsHardSecuredServlet", urlPatterns = {ServletSecurityRoleNamesCommon
            .HARD_SECURED_INDEX}, loadOnStartup = 1)
    @ServletSecurity(@HttpConstraint(value = ServletSecurity.EmptyRoleSemantic.DENY, rolesAllowed = {}))
    public static class RoleNamesAnnotationsHardSecuredServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write(ServletSecurityRoleNamesCommon.HARD_SECURED_INDEX_CONTENT);
        }
    }
}
