/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.subsystem;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.wildfly.security.http.oidc.OidcSecurityContext;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;



/**
 * Modified version of {@link SimpleServlet}. Checks token to find claims obtained using scope values in OIDC authentication
 *
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
 */
@DeclareRoles({ KeycloakConfiguration.JBOSS_ADMIN_ROLE, KeycloakConfiguration.USER_ROLE })
@ServletSecurity(@HttpConstraint(rolesAllowed = { KeycloakConfiguration.JBOSS_ADMIN_ROLE, KeycloakConfiguration.USER_ROLE }))
@WebServlet(SimpleServletWithScope.SERVLET_PATH)
public class SimpleServletWithScope extends SimpleServlet{

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;
    public static final String SERVLET_PATH = "/SimpleServletWithScope";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        if (scope.contains("microprofile-jwt")){
            message.add("microprofile-jwt: " + oidcSecurityContext.getToken().getClaimValueAsString("groups") + "\n");
        }
    }
}

