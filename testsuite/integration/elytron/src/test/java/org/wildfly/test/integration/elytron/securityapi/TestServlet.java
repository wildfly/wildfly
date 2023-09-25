/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.securityapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns="/test")
public class TestServlet extends HttpServlet {

    @Inject
    private SecurityContext securityContext;

    @EJB
    private WhoAmI whoami;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doGet");
        final PrintWriter writer = resp.getWriter();

        final String sourceParam = req.getParameter("source");
        final boolean ejb = Boolean.valueOf(req.getParameter("ejb"));
        final String source = sourceParam != null ? sourceParam : "";
        // Default Action
        final Principal principal;
        switch (source) {
            case "SecurityContext":
                principal = ejb ? whoami.getCallerPrincipalSecurityContext() : securityContext.getCallerPrincipal();
                break;
            case "SecurityDomain":
                principal = ejb ? whoami.getCallerPrincipalSecurityDomain() : SecurityDomain.getCurrent().getCurrentSecurityIdentity().getPrincipal();
                break;
            default:
                principal = ejb ? whoami.getCallerPrincipalSessionContext() : req.getUserPrincipal();
        }

        writer.print(principal == null ? "null" : principal.getName());
    }

}
