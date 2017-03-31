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

package org.wildfly.extension.undertow.security;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.AuthorizationManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SecurityRoleRef;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.SingleConstraintMatch;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.as.core.security.SimplePrincipal;
import org.jboss.security.SecurityContext;
import org.jboss.security.authorization.ResourceKeys;
import org.jboss.security.javaee.AbstractWebAuthorizationHelper;
import org.jboss.security.javaee.SecurityHelperFactory;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class JbossAuthorizationManager implements AuthorizationManager {

    private final AuthorizationManager delegate;

    public JbossAuthorizationManager(AuthorizationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isUserInRole(String role, Account account, ServletInfo servletInfo, HttpServletRequest request, Deployment deployment) {
        boolean authzDecision = true;
        boolean baseDecision = delegate.isUserInRole(role, account, servletInfo, request, deployment);
        // if the RealmBase check has passed, then we can go to authz framework
        if (baseDecision) {
            String servletName = servletInfo.getName();
            String roleName = role;
            List<SecurityRoleRef> roleRefs = servletInfo.getSecurityRoleRefs();
            if (roleRefs != null) {
                for (SecurityRoleRef ref : roleRefs) {
                    if (ref.getLinkedRole().equals(role)) {
                        roleName = ref.getRole();
                        break;
                    }
                }
            }

            SecurityContext sc = SecurityActions.getSecurityContext();
            AbstractWebAuthorizationHelper helper = null;
            try {
                helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
            } catch (Exception e) {
                UndertowLogger.ROOT_LOGGER.noAuthorizationHelper(e);
                return false;

            }
            Subject callerSubject = sc.getUtil().getSubject();
            //if (callerSubject == null) {
            //    // During hasResourcePermission check, Catalina calls hasRole. But we have not established
            //    // a subject yet in the security context. So we will get the subject from the cached principal
            //    callerSubject = getSubjectFromRequestPrincipal(principal);
            //}

            authzDecision = helper.hasRole(roleName, account.getPrincipal(), servletName, getPrincipalRoles(account),
                    PolicyContext.getContextID(), callerSubject, new ArrayList<String>(account.getRoles()));
        }
        boolean finalDecision = baseDecision && authzDecision;
        UndertowLogger.ROOT_LOGGER.tracef("hasRole:RealmBase says: %s ::Authz framework says: %s :final= %s", baseDecision, authzDecision, finalDecision);
        //TODO: do we need audit for this?
        /*
        if (finalDecision) {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasRole");
                successAudit(principal, entries);
            }
        } else {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasRole");
                failureAudit(principal, entries);
            }
        }
        */

        return finalDecision;
    }

    private Set<Principal> getPrincipalRoles(Account account) {
        final Set<Principal> roles = new HashSet<>();
        for (String role : account.getRoles()) {
            roles.add(new SimplePrincipal(role));
        }
        return roles;
    }

    @Override
    public boolean canAccessResource(List<SingleConstraintMatch> mappedConstraints, Account account, ServletInfo servletInfo, HttpServletRequest request, Deployment deployment) {
        ServletRequestContext src = ServletRequestContext.current();
        boolean baseDecision = delegate.canAccessResource(mappedConstraints, account, servletInfo, request, deployment);
        boolean authzDecision = false;
        // if the RealmBase check has passed, then we can go to authz framework
        if (baseDecision) {
            SecurityContext sc = SecurityActions.getSecurityContext();
            Subject caller = sc.getUtil().getSubject();
            //if (caller == null) {
            //    caller = getSubjectFromRequestPrincipal(request.getPrincipal());
            //}
            Map<String, Object> contextMap = new HashMap<String, Object>();
            contextMap.put(ResourceKeys.RESOURCE_PERM_CHECK, Boolean.TRUE);
            contextMap.put("securityConstraints", mappedConstraints); //TODO? What should this be?

            AbstractWebAuthorizationHelper helper = null;
            try {
                helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
            } catch (Exception e) {
                UndertowLogger.ROOT_LOGGER.noAuthorizationHelper(e);
                return false;
            }

            ArrayList<String> roles = new ArrayList<String>();
            if(account != null) {
                roles.addAll(account.getRoles());
            }
            authzDecision = helper.checkResourcePermission(contextMap, request, src.getServletResponse(), caller, PolicyContext.getContextID(),
                    requestURI(src.getExchange()), roles);
        }
        boolean finalDecision = baseDecision && authzDecision && hasUserDataPermission(request, src.getOriginalResponse(), account, mappedConstraints);

        UndertowLogger.ROOT_LOGGER.tracef("hasResourcePermission:RealmBase says: %s ::Authz framework says: %s :final= %s", baseDecision, authzDecision, finalDecision);
        //TODO: audit?

        return finalDecision;

    }


    public boolean hasUserDataPermission(HttpServletRequest request, HttpServletResponse response, Account account, List<SingleConstraintMatch> constraints) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("securityConstraints", constraints);
        map.put(ResourceKeys.USERDATA_PERM_CHECK, Boolean.TRUE);

        SecurityContext sc = SecurityActions.getSecurityContext();
        AbstractWebAuthorizationHelper helper = null;
        try {
            helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
        } catch (Exception e) {
            UndertowLogger.ROOT_LOGGER.noAuthorizationHelper(e);
            return false;
        }

        Subject callerSubject = sc.getUtil().getSubject();
        // JBAS-6419:CallerSubject has no bearing on the user data permission check
        if (callerSubject == null) {
            callerSubject = new Subject();
        }

        ArrayList<String> roles = new ArrayList<String>();
        if(account != null) {
            roles.addAll(account.getRoles());
        }
        boolean ok = helper.hasUserDataPermission(map, request, response, PolicyContext.getContextID(), callerSubject,
                roles);

        //If the status of the response has already been changed (it is different from the default Response.SC_OK) we should not attempt to change it.
        if (!ok && response.getStatus() == HttpServletResponse.SC_OK) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ok;
    }

    @Override
    public TransportGuaranteeType transportGuarantee(TransportGuaranteeType currentConnectionGuarantee, TransportGuaranteeType configuredRequiredGuarantee, HttpServletRequest request) {
        return delegate.transportGuarantee(currentConnectionGuarantee, configuredRequiredGuarantee, request);
    }


    /**
     * Get the canonical request URI from the request mapping data requestPath
     *
     * @param request
     * @return the request URI path
     */
    protected String requestURI(HttpServerExchange request) {
        String uri = request.getRelativePath();
        if (uri == null || uri.equals("/")) {
            uri = "";
        }
        return uri;
    }

}
