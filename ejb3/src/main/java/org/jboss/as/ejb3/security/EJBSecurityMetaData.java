/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.Set;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * Holds the EJB component level security metadata.
 * <p/>
 * For per method specific security metadata, take a look at {@link EJBMethodSecurityAttribute}
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBSecurityMetaData {

    private final String ejbName;

    private final String ejbClassName;

    /**
     * The security domain for this EJB component
     */
    private final String securityDomain;

    /**
     * The run-as role (if any) for this EJB component
     */
    private final String runAsRole;

    /**
     * The roles declared (via @DeclareRoles) on this EJB component
     */
    private final Set<String> declaredRoles;

    /**
     * The run-as principal (if any) for this EJB component
     */
    private final String runAsPrincipal;

    /**
     * Roles mapped with security-role
     */
    private final SecurityRolesMetaData securityRoles;

    /**
     * @param componentConfiguration Component configuration of the EJB component
     */
    public EJBSecurityMetaData(final ComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw MESSAGES.invalidComponentConfiguration(componentConfiguration.getComponentName());
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        this.ejbClassName = ejbComponentDescription.getEJBClassName();
        this.ejbName = ejbComponentDescription.getEJBName();
        this.runAsRole = ejbComponentDescription.getRunAs();
        this.securityDomain = ejbComponentDescription.getSecurityDomain();
        this.runAsPrincipal = ejbComponentDescription.getRunAsPrincipal();
        this.securityRoles = ejbComponentDescription.getSecurityRoles();
        // @DeclareRoles
        final Set<String> roles = ejbComponentDescription.getDeclaredRoles();
        this.declaredRoles = roles == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(roles);

    }

    /**
     * Returns the roles that have been declared by the bean. Returns an empty set if there are no declared roles.
     *
     * @return
     */
    public Set<String> getDeclaredRoles() {
        return declaredRoles;
    }

    /**
     * Returns the run-as role associated with this bean. Returns null if there's no run-as role associated.
     *
     * @return
     */
    public String getRunAs() {
        return this.runAsRole;
    }

    /**
     * Returns the security domain associated with the bean
     *
     * @return
     */
    public String getSecurityDomain() {
        return this.securityDomain;
    }

    /**
     * Returns the run-as principal associated with this bean. Returns 'anonymous' if no principal was set.
     *
     * @return
     */
    public String getRunAsPrincipal() {
        return runAsPrincipal;
    }

    /**
     * Returns the security-role mapping.
     *
     * @return
     */
    public SecurityRolesMetaData getSecurityRoles() {
        return securityRoles;
    }

}
