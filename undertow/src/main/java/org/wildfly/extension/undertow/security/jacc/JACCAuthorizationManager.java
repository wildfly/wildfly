/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security.jacc;

import java.security.CodeSource;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

import io.undertow.security.idm.Account;
import io.undertow.servlet.api.AuthorizationManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.SingleConstraintMatch;
import io.undertow.servlet.api.TransportGuaranteeType;
import org.jboss.security.SimplePrincipal;

/**
 * <p>
 * An implementation of {@link AuthorizationManager} that uses JACC permissions to grant or deny access to web resources.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JACCAuthorizationManager implements AuthorizationManager {

    @Override
    public boolean isUserInRole(final String roleName, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, final Deployment deployment) {

        // create the WebRoleRefPermission that will be used by JACC to determine if the user has the specified role.
        final WebRoleRefPermission permission = new WebRoleRefPermission(servletInfo.getName(), roleName);

        // create a protection domain with the user roles (or account principal if no roles are found)
        final Map<String, Set<String>> principalVersusRolesMap = deployment.getDeploymentInfo().getPrincipalVersusRolesMap();
        final Principal[] principals = this.getPrincipals(account, principalVersusRolesMap);
        final CodeSource codeSource = servletInfo.getServletClass().getProtectionDomain().getCodeSource();
        final ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null, null, principals);

        // call implies in the protection domain using the constructed WebRoleRefPermission.
        return protectionDomain.implies(permission);
    }

    @Override
    public boolean canAccessResource(List<SingleConstraintMatch> constraints, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, Deployment deployment) {

        // create the WebResourcePermission that will be used by JACC to determine if access to the resource should be granted or not.
        final WebResourcePermission permission = new WebResourcePermission(this.getCanonicalURI(request), request.getMethod());

        // create a protection domain with the user roles (or account principal if no roles are found)
        final Map<String, Set<String>> principalVersusRolesMap = deployment.getDeploymentInfo().getPrincipalVersusRolesMap();
        final Principal[] principals = this.getPrincipals(account, principalVersusRolesMap);
        final CodeSource codeSource = servletInfo.getServletClass().getProtectionDomain().getCodeSource();
        final ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null, null, principals);

        return protectionDomain.implies(permission);
    }

    @Override
    public TransportGuaranteeType transportGuarantee(TransportGuaranteeType currentConnGuarantee, TransportGuaranteeType configuredRequiredGuarantee, final HttpServletRequest request) {

        final ProtectionDomain domain = new ProtectionDomain(null, null, null, null);
        final String[] httpMethod = new String[] {request.getMethod()};
        final String canonicalURI = this.getCanonicalURI(request);

        switch (currentConnGuarantee) {
            case NONE: {
                // unprotected connection - create a WebUserDataPermission without any transport guarantee.
                WebUserDataPermission permission = new WebUserDataPermission(canonicalURI, httpMethod, null);

                // if permission was implied then the unprotected connection is ok.
                if (domain.implies(permission)) {
                    return TransportGuaranteeType.NONE;
                }
                else {
                    // if permission was not implied we require protection for the connection.
                    return TransportGuaranteeType.CONFIDENTIAL;
                }
            }
            case INTEGRAL:
            case CONFIDENTIAL: {
                // we will try using both transport guarantees (CONFIDENTIAL and INTEGRAL) as SSL provides both.
                WebUserDataPermission permission = new WebUserDataPermission(canonicalURI, httpMethod,
                        TransportGuaranteeType.CONFIDENTIAL.name());

                if (domain.implies(permission)) {
                    return TransportGuaranteeType.CONFIDENTIAL;
                }
                else {
                    // try with the INTEGRAL connection guarantee type.
                    permission = new WebUserDataPermission(canonicalURI, httpMethod, TransportGuaranteeType.INTEGRAL.name());
                    if (domain.implies(permission)) {
                        return TransportGuaranteeType.INTEGRAL;
                    }
                    else {
                        return TransportGuaranteeType.REJECTED;
                    }
                }
            }
            default:
                return TransportGuaranteeType.REJECTED;
        }
    }

    /**
     * <p>
     * Gets the canonical request URI - that is, the request URI minus the context path.
     * </p>
     *
     * @param request the {@link HttpServletRequest} for which we want the canonical URI.
     * @return the constructed canonical URI.
     */
    private String getCanonicalURI(HttpServletRequest request) {
        String canonicalURI = request.getRequestURI().substring(request.getContextPath().length());
        if (canonicalURI == null || canonicalURI.equals("/"))
            canonicalURI = "";
        return canonicalURI;
    }

    /**
     * <p>
     * Merges the roles found in the specified account parameter with the mapped roles in the second parameter and returns
     * the resulting set as a {@link Principal} array.
     * </p>
     *
     * @param account the authenticated user account.
     * @param principalVersusRolesMap the principal to roles map as configured in the deployment.
     * @return a {@link Principal}[] containing the merged roles. If the specified account is {@code null}, this method
     * returns {@code null}. If the account is not null but no roles can be associated with the account principal, then
     * the account principal is returned.
     */
    private Principal[] getPrincipals(Account account, Map<String, Set<String>> principalVersusRolesMap) {

        if (account == null)
            return null;

        final Set<String> mappedRoles = principalVersusRolesMap.get(account.getPrincipal().getName());

        // create a set that merges the account roles with deployment roles (if any)
        final Set<Principal> roles = new HashSet<Principal>();
        for (String role : account.getRoles())
            roles.add(new SimplePrincipal(role));
        if (mappedRoles != null) {
            for (String role : mappedRoles)
                roles.add(new SimplePrincipal(role));
        }

        if (roles.isEmpty())
            return new Principal[] {account.getPrincipal()};
        return roles.toArray(new Principal[roles.size()]);
    }
}
