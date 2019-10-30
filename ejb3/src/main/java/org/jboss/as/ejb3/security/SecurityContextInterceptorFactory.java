/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import java.util.Map;
import java.util.Set;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Anil Saldhana
 */
public class SecurityContextInterceptorFactory extends ComponentInterceptorFactory {

    private static final String DEFAULT_DOMAIN = "other";

    private final boolean securityRequired;
    private final boolean propagateSecurity;
    private final String policyContextID;

    public SecurityContextInterceptorFactory(final boolean securityRequired, final String policyContextID) {
        this(securityRequired, true, policyContextID);
    }

    public SecurityContextInterceptorFactory(final boolean securityRequired, final boolean propagateSecurity, final String policyContextID) {
        this.securityRequired = securityRequired;
        this.propagateSecurity = propagateSecurity;
        this.policyContextID = policyContextID;
    }

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (component instanceof EJBComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }
        final EJBComponent ejbComponent = (EJBComponent) component;
        final ServerSecurityManager securityManager;
        if(propagateSecurity) {
            securityManager = ejbComponent.getSecurityManager();
        } else {
            securityManager = new SimpleSecurityManager((SimpleSecurityManager) ejbComponent.getSecurityManager());
        }
        final EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();
        String securityDomain =  securityMetaData.getSecurityDomain();
        if (securityDomain == null) {
            securityDomain = DEFAULT_DOMAIN;
        }
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.trace("Using security domain: " + securityDomain + " for EJB " + ejbComponent.getComponentName());
        }
        final String runAs = securityMetaData.getRunAs();
        // TODO - We should do something with DeclaredRoles although it never has much meaning in JBoss AS
        final String runAsPrincipal = securityMetaData.getRunAsPrincipal();
        final SecurityRolesMetaData securityRoles = securityMetaData.getSecurityRoles();
        Set<String> extraRoles = null;
        Map<String,Set<String>> principalVsRolesMap = null;
        if (securityRoles != null) {
            principalVsRolesMap = securityRoles.getPrincipalVersusRolesMap();
            if (runAsPrincipal != null)
                extraRoles = securityRoles.getSecurityRoleNamesByPrincipal(runAsPrincipal);
        }
        SecurityContextInterceptorHolder holder = new SecurityContextInterceptorHolder();
        holder.setSecurityManager(securityManager).setSecurityDomain(securityDomain)
        .setRunAs(runAs).setRunAsPrincipal(runAsPrincipal).setPolicyContextID(this.policyContextID)
        .setExtraRoles(extraRoles).setPrincipalVsRolesMap(principalVsRolesMap)
        .setSkipAuthentication(securityRequired == false);

        return new SecurityContextInterceptor(holder);
    }
}