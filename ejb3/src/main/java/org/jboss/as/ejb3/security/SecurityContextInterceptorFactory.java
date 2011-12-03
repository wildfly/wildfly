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

import java.util.Set;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SecurityContextInterceptorFactory extends ComponentInterceptorFactory {


    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (component instanceof EJBComponent == false) {
            throw MESSAGES.unexpectedComponent(component, EJBComponent.class);
        }
        final EJBComponent ejbComponent = (EJBComponent) component;
        final SimpleSecurityManager securityManager = ejbComponent.getSecurityManager();
        final EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();
        final String securityDomain = securityMetaData.getSecurityDomain();
        if (securityDomain == null) {
            throw MESSAGES.invalidSecurityForDomainSet(ejbComponent.getComponentName());
        }
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.trace("Using security domain: " + securityDomain + " for EJB " + ejbComponent.getComponentName());
        }
        final String runAs = securityMetaData.getRunAs();
        // TODO - We should do something with DeclaredRoles although it never has much meaning in JBoss AS
        final String runAsPrincipal = securityMetaData.getRunAsPrincipal();
        final SecurityRolesMetaData securityRoles = securityMetaData.getSecurityRoles();
        Set<String> extraRoles = null;
        if (securityRoles != null && runAsPrincipal != null) {
            extraRoles = securityRoles.getSecurityRoleNamesByPrincipal(runAsPrincipal);
        }
        return new SecurityContextInterceptor(securityManager, securityDomain, runAs, runAsPrincipal, extraRoles);
    }
}
