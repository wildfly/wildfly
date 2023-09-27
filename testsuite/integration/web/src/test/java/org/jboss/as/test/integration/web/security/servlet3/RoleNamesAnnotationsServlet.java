/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet3;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
