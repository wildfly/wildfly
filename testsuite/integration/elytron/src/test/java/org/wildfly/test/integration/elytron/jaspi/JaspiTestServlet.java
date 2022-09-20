/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.jaspi;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.ejb.EJB;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.jaspi.JaspiConfigurationBuilder;

/**
 * A servlet used for JASPI testing.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JaspiTestServlet extends HttpServlet {

    private volatile String registrationId;

    @EJB
    private WhoAmI whoAmIBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PrintWriter writer = resp.getWriter();

        final String action = req.getParameter("action");
        if (action != null) {
            switch (action) {
                case "register":
                    ServletContext servletContext = req.getServletContext();
                    registrationId = JaspiConfigurationBuilder
                            .builder("HttpServlet", servletContext.getVirtualServerName() + " " + servletContext.getContextPath())
                            .addAuthModuleFactory(SimpleServerAuthModule::new)
                            .register();
                    writer.print("REGISTERED");
                    return;
                case "remove":
                    if (registrationId == null) {
                        throw new IllegalStateException("No registration to remove.");
                    }
                    AuthConfigFactory authConfigFactory = AuthConfigFactory.getFactory();
                    authConfigFactory.removeRegistration(registrationId);
                    registrationId = null;
                    writer.print("REMOVED");
                    return;
                case "ejb":
                    writer.print(String.valueOf(whoAmIBean.getCallerPrincipal()));
                    return;
            }
        }

        final String value = req.getParameter("value");
        if ("authType".equals(value)) {
            writer.print(req.getAuthType());
            return;
        }
        writer.print(String.valueOf(req.getUserPrincipal()));
    }

}
