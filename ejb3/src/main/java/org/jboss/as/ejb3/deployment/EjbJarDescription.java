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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;

/**
 * User: jpai
 */
public class EjbJarDescription {

    private final EEModuleDescription eeModuleDescription;

    private final Set<String> applicationLevelSecurityRoles = new HashSet<String>();

    private final EEApplicationClasses applicationClassesDescription;

    /**
     * True if this represents EJB's packaged in a war
     */
    private final boolean war;

    public EjbJarDescription(EEModuleDescription eeModuleDescription, final EEApplicationClasses applicationClassesDescription, boolean war) {
        this.war = war;
        if (eeModuleDescription == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("EE module description");
        }
        this.eeModuleDescription = eeModuleDescription;
        this.applicationClassesDescription = applicationClassesDescription;
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



    /**
     * Returns the {@link SessionBeanComponentDescription session beans} belonging to this {@link EjbJarDescription}.
     * <p/>
     * Returns an empty collection if no session beans exist
     *
     * @return
     */
    public Collection<SessionBeanComponentDescription> getSessionBeans() {
        Collection<SessionBeanComponentDescription> sessionBeans = new ArrayList<SessionBeanComponentDescription>();
        for (ComponentDescription componentDescription : this.eeModuleDescription.getComponentDescriptions()) {
            if (componentDescription instanceof SessionBeanComponentDescription) {
                sessionBeans.add((SessionBeanComponentDescription) componentDescription);
            }
        }
        return sessionBeans;
    }

    public Collection<MessageDrivenComponentDescription> getMessageDrivenBeans() {
        Collection<MessageDrivenComponentDescription> mdbs = new ArrayList<MessageDrivenComponentDescription>();
        for (ComponentDescription componentDescription : this.eeModuleDescription.getComponentDescriptions()) {
            if (componentDescription instanceof MessageDrivenComponentDescription) {
                mdbs.add((MessageDrivenComponentDescription) componentDescription);
            }
        }
        return mdbs;
    }

    public void addSessionBeans(Collection<SessionBeanComponentDescription> sessionBeans) {
        for (SessionBeanComponentDescription sessionBean : sessionBeans) {
            this.eeModuleDescription.addComponent(sessionBean);
        }
    }

    public boolean isWar() {
        return war;
    }

    public EEApplicationClasses getApplicationClassesDescription() {
        return applicationClassesDescription;
    }
}
