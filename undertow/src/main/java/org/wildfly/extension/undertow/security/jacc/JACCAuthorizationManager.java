/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static java.security.AccessController.doPrivileged;

import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
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
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * <p>
 * An implementation of {@link AuthorizationManager} that uses JACC permissions to grant or deny access to web resources.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JACCAuthorizationManager implements AuthorizationManager {

    public static final AuthorizationManager INSTANCE = new JACCAuthorizationManager();

    @Override
    public boolean isUserInRole(final String roleName, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, final Deployment deployment) {
        return hasPermission(account, deployment, servletInfo, new WebRoleRefPermission(servletInfo.getName(), roleName));
    }

    @Override
    public boolean canAccessResource(List<SingleConstraintMatch> constraints, final Account account, final ServletInfo servletInfo, final HttpServletRequest request, Deployment deployment) {
        return hasPermission(account, deployment, servletInfo, new WebResourcePermission(request));
    }

    @Override
    public TransportGuaranteeType transportGuarantee(TransportGuaranteeType currentConnGuarantee, TransportGuaranteeType configuredRequiredGuarantee, final HttpServletRequest request) {
        final ProtectionDomain domain = new ProtectionDomain(null, null, null, null);
        final String[] httpMethod = new String[] {request.getMethod()};
        final String canonicalURI = getCanonicalURI(request);

        switch (currentConnGuarantee) {
            case NONE: {
                // unprotected connection - create a WebUserDataPermission without any transport guarantee.
                WebUserDataPermission permission = new WebUserDataPermission(canonicalURI, httpMethod, null);

                // if permission was implied then the unprotected connection is ok.
                if (hasPermission(domain, permission)) {
                    return TransportGuaranteeType.NONE;
                }
                else {
                    permission = new WebUserDataPermission(canonicalURI, httpMethod, TransportGuaranteeType.CONFIDENTIAL.name());
                    // permission is only granted with CONFIDENTIAL
                    if (hasPermission(domain, permission)) {
                        return TransportGuaranteeType.CONFIDENTIAL;
                    }
                    //either way we just don't have permission, let the request proceed and be rejected later
                    return TransportGuaranteeType.NONE;
                }
            }
            case INTEGRAL:
            case CONFIDENTIAL: {
                // we will try using both transport guarantees (CONFIDENTIAL and INTEGRAL) as SSL provides both.
                WebUserDataPermission permission = new WebUserDataPermission(canonicalURI, httpMethod,
                        TransportGuaranteeType.CONFIDENTIAL.name());

                if (hasPermission(domain, permission)) {
                    return TransportGuaranteeType.CONFIDENTIAL;
                }
                else {
                    // try with the INTEGRAL connection guarantee type.
                    permission = new WebUserDataPermission(canonicalURI, httpMethod, TransportGuaranteeType.INTEGRAL.name());
                    if (hasPermission(domain, permission)) {
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

    private boolean hasPermission(Account account, Deployment deployment, ServletInfo servletInfo, Permission permission) {
        CodeSource codeSource = servletInfo.getServletClass().getProtectionDomain().getCodeSource();
        ProtectionDomain domain = new ProtectionDomain(codeSource, null, null, getGrantedRoles(account, deployment));
        return hasPermission(domain, permission);
    }

    private boolean hasPermission(ProtectionDomain domain, Permission permission) {
        Policy policy = WildFlySecurityManager.isChecking() ? doPrivileged((PrivilegedAction<Policy>) Policy::getPolicy) : Policy.getPolicy();
        return policy.implies(domain, permission);
    }

    private Principal[] getGrantedRoles(Account account, Deployment deployment) {
        if (account == null) {
            return new Principal[] {};
        }

        Set<String> roles = new HashSet<>(account.getRoles());
        Map<String, Set<String>> principalVersusRolesMap = deployment.getDeploymentInfo().getPrincipalVersusRolesMap();

        roles.addAll(principalVersusRolesMap.getOrDefault(account.getPrincipal().getName(), Collections.emptySet()));

        Principal[] principals = new Principal[roles.size()];
        int index = 0;
        for (String role : roles) {
            principals[index++] = () -> role;
        }
        return principals;
    }
}
