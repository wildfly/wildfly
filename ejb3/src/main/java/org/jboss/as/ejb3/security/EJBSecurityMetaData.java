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

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;

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
     * Security role links. The key is the "from" role name and the value is a collection of "to" role names of the link.
     */
    private final Map<String, Collection<String>> securityRoleLinks;

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
        this.securityRoleLinks = ejbComponentDescription.getSecurityRoleLinks();
        // @DeclareRoles
        final Set<String> roles = ejbComponentDescription.getDeclaredRoles();
        this.declaredRoles = roles == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(roles);

        // The jboss-ejb3.xml allows for mapping a security role to a principal:
        // <security-role>
        //  <principal-name>fooPrincipal</principal-name>
        //  <role-name>Administrator</role-name>
        // </security-role>
        //
        // The ejb-jar.xml allows for linking a "x" role to a "y" alias role via the security-role-ref element:
        //
        // <security-role-ref>
        //  <role-name>ADM</role-name>
        //  <role-link>Administrator</role-link>
        // </security-role-ref>
        //
        // The code here ensures that we setup the appropriate metadata to ensure that the "fooPrincipal" prinicipal
        // is mapped to the role alias "ADM".
        // So ultimately we will have a fooPrincipal -> Administrator and fooPrincipal -> ADM mapping in our security roles
        if (this.securityRoleLinks != null && this.securityRoles != null) {
            for (final Map.Entry<String, Collection<String>> entry : this.securityRoleLinks.entrySet()) {
                final String aliasRoleName = entry.getKey();
                // get the real role names for this alias
                final Collection<String> roleNames = entry.getValue();
                if (roleNames == null || roleNames.isEmpty()) {
                    continue;
                }
                // for each of these roles, see if we have a role name to principals mapping
                for (final String roleName : roleNames) {
                    if (roleName == null || roleName.isEmpty()) {
                        continue;
                    }
                    final SecurityRoleMetaData securityRole = this.getSecurityRole(roleName);
                    if (securityRole == null) {
                        continue;
                    }
                    // found a role name to principal(s) mapping, we'll now create a mapping between these
                    // principals to the alias role
                    final Set<String> principals = securityRole.getPrincipals();
                    if (principals == null || principals.isEmpty()) {
                        continue;
                    }
                    // create a new security role which maps the principals to the alias role
                    final SecurityRoleMetaData aliasSecurityRole = new SecurityRoleMetaData();
                    aliasSecurityRole.setRoleName(aliasRoleName);
                    aliasSecurityRole.setPrincipals(principals);
                    // add this new security role to our existing security role collection for this bean
                    this.securityRoles.add(aliasSecurityRole);
                }
            }
        }
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

    /**
     * Returns the {@link SecurityRoleMetaData} for the passed <code>roleName</code>, from the collection of security
     * roles configured for this bean. Returns null if there's no security role for the role name.
     *
     * @param roleName The role name
     * @return
     */
    private SecurityRoleMetaData getSecurityRole(final String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            EjbMessages.MESSAGES.stringParamCannotBeNullOrEmpty("Role name");
        }
        if (this.securityRoles == null) {
            return null;
        }
        for (final SecurityRoleMetaData securityRole : this.securityRoles) {
            if (roleName.equals(securityRole.getRoleName())) {
                return securityRole;
            }
        }
        return null;
    }

}
