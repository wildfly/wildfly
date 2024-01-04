/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
