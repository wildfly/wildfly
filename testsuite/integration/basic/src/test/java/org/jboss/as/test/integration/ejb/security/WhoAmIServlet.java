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
package org.jboss.as.test.integration.ejb.security;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;

import javax.annotation.security.DeclareRoles;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.integration.ejb.security.Util;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns = "/whoAmI", loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Users" }))
@DeclareRoles("Users")
public class WhoAmIServlet extends HttpServlet {
    @EJB
    private Entry bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.toString());
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
