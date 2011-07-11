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

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;

import javax.ejb.ApplicationException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: jpai
 */
public class EjbJarDescription {

    private final EEModuleDescription eeModuleDescription;

    private final Map<String, ApplicationException> applicationExceptions = new ConcurrentHashMap();

    private final Set<String> applicationLevelSecurityRoles = new HashSet<String>();

    private final EEApplicationClasses applicationClassesDescription;

    /**
     * True if this represents EJB's packaged in a war
     */
    private final boolean war;

    public EjbJarDescription(EEModuleDescription eeModuleDescription, final EEApplicationClasses applicationClassesDescription, boolean war) {
        this.war = war;
        if (eeModuleDescription == null) {
            throw new IllegalArgumentException(EEModuleDescription.class.getSimpleName() + " cannot be null");
        }
        this.eeModuleDescription = eeModuleDescription;
        this.applicationClassesDescription = applicationClassesDescription;
    }

    public void addSecurityRole(final String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Security role name cannot be null or empty: " + role);
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

    public void addApplicationException(final String exceptionClassName, final boolean rollback, final boolean inherited) {
        if (exceptionClassName == null || exceptionClassName.isEmpty()) {
            throw new IllegalArgumentException("Invalid exception class name: " + exceptionClassName);
        }
        //TODO: Is this a good idea? ApplicationException's equals/hashCode
        //will not work the way that would be expected
        ApplicationException appException = new ApplicationException() {
            @Override
            public boolean inherited() {
                return inherited;
            }

            @Override
            public boolean rollback() {
                return rollback;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ApplicationException.class;
            }
        };
        // add it to the map
        this.applicationExceptions.put(exceptionClassName, appException);
    }

    public boolean hasComponent(String componentName) {
        return eeModuleDescription.hasComponent(componentName);
    }

    public EEModuleDescription getEEModuleDescription() {
        return this.eeModuleDescription;
    }

    Map<String, ApplicationException> getApplicationExceptions() {
        return Collections.unmodifiableMap(this.applicationExceptions);
    }

    public EjbJarConfiguration createEjbJarConfiguration(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        EjbJarConfiguration ejbDeploymentConfiguration = new EjbJarConfiguration(this);
        prepareEjbJarConfiguration(ejbDeploymentConfiguration, phaseContext);
        return ejbDeploymentConfiguration;
    }

    protected void prepareEjbJarConfiguration(EjbJarConfiguration ejbDeploymentConfiguration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            throw new IllegalStateException("Module not yet set in deployment unit " + deploymentUnit);
        }
        ClassLoader moduleClassLoader = module.getClassLoader();
        // setup application exceptions
        this.prepareApplicationExceptions(ejbDeploymentConfiguration, moduleClassLoader);

    }

    private void prepareApplicationExceptions(EjbJarConfiguration ejbDeploymentConfiguration, ClassLoader classLoader) throws DeploymentUnitProcessingException {
        for (Map.Entry<String, ApplicationException> entry : this.applicationExceptions.entrySet()) {
            String applicationExceptionClass = entry.getKey();
            try {
                Class<?> exceptionClass = classLoader.loadClass(applicationExceptionClass);
                ejbDeploymentConfiguration.addApplicationException(exceptionClass, entry.getValue());
            } catch (ClassNotFoundException cnfe) {
                throw new DeploymentUnitProcessingException(cnfe);
            }
        }
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
