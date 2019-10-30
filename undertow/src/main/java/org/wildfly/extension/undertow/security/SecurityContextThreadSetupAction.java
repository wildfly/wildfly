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

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.mapping.MappingContext;
import org.jboss.security.mapping.MappingManager;
import org.jboss.security.mapping.MappingType;
import org.wildfly.security.manager.WildFlySecurityManager;
import io.undertow.servlet.api.ThreadSetupHandler;

/**
 * Thread setup action that sets up the security context. If it already exists then it will be re-used, otherwise
 * a new one is created.
 *
 * @author Stuart Douglas
 */
public class SecurityContextThreadSetupAction implements ThreadSetupHandler {

    private final String securityDomain;
    private final SecurityDomainContext securityDomainContext;
    private final Map<String, Set<String>> principleVsRoleMap;

    private static final PrivilegedAction<Object> TEAR_DOWN_PA = new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            SecurityActions.clearSecurityContext();
            SecurityRolesAssociation.setSecurityRoles(null);
            return null;
        }
    };

    public SecurityContextThreadSetupAction(final String securityDomain, SecurityDomainContext securityDomainContext, Map<String, Set<String>> principleVsRoleMap) {
        this.securityDomain = securityDomain;
        this.securityDomainContext = securityDomainContext;
        this.principleVsRoleMap = principleVsRoleMap;

    }

    @Override
    public <T, C> Action<T, C> create(Action<T, C> action) {
        return (exchange, context) -> {
            SecurityContext sc = null;
            if (exchange != null) {
                sc = exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT);
            }
            if (sc == null) {
                sc = SecurityActions.createSecurityContext(securityDomain);
                if (exchange != null) {
                    exchange.putAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT, sc);
                }
            }
            SecurityActions.setSecurityContextOnAssociation(sc);
            final MappingManager mappingManager = securityDomainContext.getMappingManager();

            if (mappingManager != null) {
                if (WildFlySecurityManager.isChecking()) {
                    WildFlySecurityManager.doUnchecked(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            // if there are mapping modules let them handle the role mapping
                            MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                            if (mc != null && mc.hasModules()) {
                                SecurityRolesAssociation.setSecurityRoles(principleVsRoleMap);
                            }
                            return null;
                        }
                    });
                } else {
                    // if there are mapping modules let them handle the role mapping
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc != null && mc.hasModules()) {
                        SecurityRolesAssociation.setSecurityRoles(principleVsRoleMap);
                    }
                }
            }
            try {
                return action.call(exchange, context);
            } finally {
                if (WildFlySecurityManager.isChecking()) {
                    WildFlySecurityManager.doUnchecked(TEAR_DOWN_PA);
                } else {
                    SecurityActions.clearSecurityContext();
                    SecurityRolesAssociation.setSecurityRoles(null);
                }
            }
        };
    }
}
