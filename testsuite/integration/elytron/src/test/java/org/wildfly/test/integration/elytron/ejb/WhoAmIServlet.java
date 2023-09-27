/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.Callable;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.integration.ejb.security.Util;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns = "/whoAmI", loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Users" }))
@DeclareRoles("Users")
public class WhoAmIServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private Entry bean;

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
                Callable<Void> callable = () -> {
                    writer.write(bean.whoAmI());
                    return null;
                };
                Util.switchIdentity(username, password, callable);
            } catch (Exception e) {
                throw new IOException("Unexpected failure", e);
            }
        } else if ("doubleWhoAmI".equals(method)) {
            String[] response;
            try {
                if (username != null && password != null) {
                    response = bean.doubleWhoAmI(username, password);
                } else {
                    response = bean.doubleWhoAmI();
                }
            } catch (EJBException e) {
                resp.setStatus(SC_FORBIDDEN);
                e.printStackTrace(new PrintWriter(writer));
                return;
            } catch (Exception e) {
                throw new ServletException("Unexpected failure", e);
            }
            writer.write(response[0] + "," + response[1]);
        } else if ("doIHaveRole".equals(method)) {
            try {
                Callable<Void> callable = () -> {
                    writer.write(String.valueOf(bean.doIHaveRole(role)));
                    return null;
                };
                Util.switchIdentity(username, password, callable);
            } catch (Exception e) {
                throw new IOException("Unexpected failure", e);
            }
        } else if ("doubleDoIHaveRole".equals(method)) {
            try {
                boolean[] response = null;
                if (username != null && password != null) {
                    response = bean.doubleDoIHaveRole(role, username, password);
                } else {
                    response = bean.doubleDoIHaveRole(role);
                }
                writer.write(String.valueOf(response[0]) + "," + String.valueOf(response[1]));
            } catch (Exception e) {
                throw new ServletException("Unexpected Failure", e);
            }
        } else {
            throw new IllegalArgumentException("Parameter 'method' either missing or invalid method='" + method + "'");
        }

    }
}
