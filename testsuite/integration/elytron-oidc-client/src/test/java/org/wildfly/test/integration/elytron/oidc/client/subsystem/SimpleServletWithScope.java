/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2023, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.oidc.client.subsystem;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.wildfly.security.http.oidc.OidcSecurityContext;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;


/**
 * Modified version of {@link SimpleServlet}. Checks token to find claims obtained using scope values in OIDC authentication
 *
 * @author Prarthona Paul
 */
@DeclareRoles({ KeycloakConfiguration.JBOSS_ADMIN_ROLE })
@ServletSecurity(@HttpConstraint(rolesAllowed = { KeycloakConfiguration.JBOSS_ADMIN_ROLE }))
@WebServlet(SimpleServletWithScope.SERVLET_PATH)
public class SimpleServletWithScope extends SimpleServlet{

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;
    public static final String SERVLET_PATH = "/SimpleServletWithScope";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        OidcSecurityContext oidcSecurityContext = getOidcSecurityContext(req);
        final PrintWriter writer = resp.getWriter();

        List<String> scopeClaims = new ArrayList<>();
        if (oidcSecurityContext != null) {
            getScopeClaims(scopeClaims, oidcSecurityContext);
        }
        writer.write(RESPONSE_BODY);
        if (scopeClaims.isEmpty()) {
            writer.write("No additional claims were recovered using the scope values.\n" );
        } else {
            writer.write("Claims received using additional scope values: \n" );
            for (String value : scopeClaims) {
                writer.write(value);
            }
        }
        writer.close();
    }

    private OidcSecurityContext getOidcSecurityContext(HttpServletRequest req) {
        return (OidcSecurityContext) req.getAttribute(OidcSecurityContext.class.getName());
    }

    private void getScopeClaims(List<String> message, OidcSecurityContext oidcSecurityContext) {
        String scope = oidcSecurityContext.getToken().getClaimValueAsString("scope");
        if (scope == null){
            return;
        }
        if (scope.contains("profile")){
            message.add("profile: " + oidcSecurityContext.getToken().getClaimValueAsString("given_name") + " "
                    + oidcSecurityContext.getToken().getClaimValueAsString("family_name"));
        }
        if (scope.contains("email")){
            message.add("email: " + oidcSecurityContext.getToken().getClaimValueAsString("email_verified") + "\n");
        }
    }
}

