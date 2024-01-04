/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.Map;
import java.util.Set;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.RoleMapper;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class SecurityRolesAddingInterceptor implements Interceptor {
    private final String category;
    private final Map<String, Set<String>> principalVsRolesMap;

    public SecurityRolesAddingInterceptor(final String category, final Map<String,Set<String>> principalVsRolesMap) {
        this.category = category;
        this.principalVsRolesMap = principalVsRolesMap;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final SecurityDomain securityDomain = context.getPrivateData(SecurityDomain.class);
        Assert.checkNotNullParam("securityDomain", securityDomain);
        final SecurityIdentity currentIdentity = securityDomain.getCurrentSecurityIdentity();
        final Set<String> securityRoles = principalVsRolesMap.get(currentIdentity.getPrincipal().getName());
        if (securityRoles != null && ! securityRoles.isEmpty()) {
            final RoleMapper roleMapper = RoleMapper.constant(Roles.fromSet(securityRoles));
            final RoleMapper mergeMapper = roleMapper.or((roles) -> currentIdentity.getRoles(category));
            final SecurityIdentity newIdentity;
            if(WildFlySecurityManager.isChecking()) {
                newIdentity = AccessController.doPrivileged((PrivilegedAction<SecurityIdentity>) () -> currentIdentity.withRoleMapper(category, mergeMapper));
            } else {
                newIdentity = currentIdentity.withRoleMapper(category, mergeMapper);
            }
            try {
                return newIdentity.runAs(context);
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if(cause != null) {
                    if(cause instanceof Exception) {
                        throw (Exception) cause;
                    } else {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw e;
                }
            }
        } else {
            return context.proceed();
        }
    }
}
