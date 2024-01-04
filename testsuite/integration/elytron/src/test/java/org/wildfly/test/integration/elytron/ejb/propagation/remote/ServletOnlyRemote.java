/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.propagation.remote;

import java.io.IOException;
import java.io.Writer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.security.DeclareRoles;
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
public class ServletOnlyRemote extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        Writer writer = resp.getWriter();
        String method = req.getParameter("method");
        String role = req.getParameter("role");

        if ("whoAmI".equals(method)) {
            try {
                EntryRemote bean = lookup(EntryRemote.class, "java:global/ear-ejb-deployment-remote/ear-ejb-deployment-remote-ejb/EntryBeanRemote!org.wildfly.test.integration.elytron.ejb.propagation.remote.EntryRemote");
                writer.write(bean.whoAmI());
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else if ("doIHaveRole".equals(method)) {
            try {
                EntryRemote bean = lookup(EntryRemote.class, "java:global/ear-ejb-deployment-remote/ear-ejb-deployment-remote-ejb/EntryBeanRemote!org.wildfly.test.integration.elytron.ejb.propagation.remote.EntryRemote");
                writer.write(String.valueOf(bean.doIHaveRole(role)));
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else {
            throw new IllegalArgumentException("Parameter 'method' either missing or invalid method='" + method + "'");
        }
    }

    public static <T> T lookup(Class<T> clazz, String jndiName) {
        Object bean = lookup(jndiName);
        return clazz.cast(bean);
    }

    private static Object lookup(String jndiName) {
        Context context = null;
        try {
            context = new InitialContext();
            return context.lookup(jndiName);
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        } finally {
            try {
                context.close();
            } catch (NamingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
