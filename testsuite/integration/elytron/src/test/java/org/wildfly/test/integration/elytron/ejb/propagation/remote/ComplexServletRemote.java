/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.propagation.remote;


import java.io.IOException;
import java.io.Writer;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@WebServlet(urlPatterns = "/whoAmI", loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Users" }))
@DeclareRoles("Users")
public class ComplexServletRemote extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private ManagementBeanRemote bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        Writer writer = resp.getWriter();
        String method = req.getParameter("method");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String role = req.getParameter("role");

        if ("whoAmI".equals(method)) {
            try {
                writer.write(bean.whoAmI());
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else if ("invokeEntryDoIHaveRole".equals(method)) {
            try {
            writer.write(String.valueOf(bean.invokeEntryDoIHaveRole(role)));
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else if ("switchThenInvokeEntryDoIHaveRole".equals(method)) {
            try {
                String[] result = bean.switchThenInvokeEntryDoIHaveRole(username, password, role);
                writer.write(result[0] + "," + result[1]);
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else {
            throw new IllegalArgumentException("Parameter 'method' either missing or invalid method='" + method + "'");
        }

    }
}
