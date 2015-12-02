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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.authz.RoleMapper;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ElytronInterceptorFactory extends ComponentInterceptorFactory {

    private static final String DEFAULT_DOMAIN = "ApplicationDomain";

    private final String policyContextID;

    public ElytronInterceptorFactory(final String policyContextID) {
        this.policyContextID = policyContextID;
    }

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (! (component instanceof EJBComponent)) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }

        final EJBComponent ejbComponent = (EJBComponent) component;
        final EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();

        final ArrayList<Interceptor> interceptors = new ArrayList<>(2);

        // first interceptor: security domain association
        String securityDomainName =  securityMetaData.getSecurityDomain();
        if (securityDomainName == null) {
            securityDomainName = DEFAULT_DOMAIN;
        }
        final Map<String, SecurityDomain> securityDomainsByName = ejbComponent.getSecurityDomainsByName();
        final SecurityDomain securityDomain = securityDomainsByName.get(securityDomainName);
        if (securityDomain == null) {
            throw EjbLogger.ROOT_LOGGER.invalidSecurityForDomainSet(ejbComponent.getComponentName());
        }
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.trace("Using security domain: " + securityDomainName + " for EJB " + ejbComponent.getComponentName());
        }
        interceptors.add(new SecurityDomainInterceptor(securityDomain));

        // next interceptor: policy context ID
        interceptors.add(new PolicyContextIdInterceptor(policyContextID));

        // need role metadata for remainder
        final SecurityRolesMetaData securityRoles = securityMetaData.getSecurityRoles();

        // next interceptor: run-as-principal
        final String runAsPrincipal = securityMetaData.getRunAsPrincipal();
        // Switch users if there's a run-as principal
        if (runAsPrincipal != null) {
            interceptors.add(new RunAsPrincipalInterceptor(runAsPrincipal));

            // next interceptor: extra principal roles
            final Set<String> extraRoles = securityRoles.getSecurityRoleNamesByPrincipal(runAsPrincipal);
            if (! extraRoles.isEmpty()) {
                interceptors.add(new RoleAddingInterceptor("ejb", RoleMapper.constant(extraRoles)));
            }
        }

        // next interceptor: run-as-role
        final String runAs = securityMetaData.getRunAs();
        if (runAs != null) {
            interceptors.add(new RoleAddingInterceptor("ejb", RoleMapper.constant(Collections.singleton(runAs))));
        }

        final Set<String> declaredRoles = securityMetaData.getDeclaredRoles();
        RoleMapper.constant(declaredRoles);

        return Interceptors.getChainedInterceptor(interceptors);
    }
}