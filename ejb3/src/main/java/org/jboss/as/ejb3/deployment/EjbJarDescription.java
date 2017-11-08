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

package org.jboss.as.ejb3.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * User: jpai
 */
public class EjbJarDescription {

    private final EEModuleDescription eeModuleDescription;

    private final Set<String> applicationLevelSecurityRoles = new HashSet<String>();

    /**
     * True if this represents EJB's packaged in a war
     */
    private final boolean war;

    public EjbJarDescription(EEModuleDescription eeModuleDescription, boolean war) {
        this.war = war;
        if (eeModuleDescription == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("EE module description");
        }
        this.eeModuleDescription = eeModuleDescription;
    }

    public void addSecurityRole(final String role) {
        if (role == null || role.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.stringParamCannotBeNullOrEmpty("Security role");
        }
        this.applicationLevelSecurityRoles.add(role);
    }

    /**
     * Returns the security roles that have been defined at the application level (via security-role elements in the
     * ejb-jar.xml). Note that this set does *not* include the roles that have been defined at each individual component
     * level (via @DeclareRoles, @RolesAllowed annotations or security-role-ref element)
     * <p/>
     * Returns an empty set if no roles have been defined at the application level.
     *
     * @return
     */
    public Set<String> getSecurityRoles() {
        return Collections.unmodifiableSet(this.applicationLevelSecurityRoles);
    }


    public boolean hasComponent(String componentName) {
        return eeModuleDescription.hasComponent(componentName);
    }

    public EEModuleDescription getEEModuleDescription() {
        return this.eeModuleDescription;
    }


    public boolean isWar() {
        return war;
    }

}
