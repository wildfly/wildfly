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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jboss.as.ejb3.deployment.processors.ViewInterfaces.getPotentialViewInterfaces;

/**
 * Processes {@link Local @Local} and {@link @Remote} annotation of a session bean and sets up the {@link SessionBeanComponentDescription}
 * out of it.
 * <p/>
 *
 * @author Jaikiran Pai
 */
public class BusinessViewAnnotationProcessor implements DeploymentUnitProcessor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(BusinessViewAnnotationProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader moduleClassLoader = module.getClassLoader();
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription instanceof SessionBeanComponentDescription == false) {
                continue;
            }
            final Class<?> ejbClass = this.getEjbClass(componentDescription.getComponentClassName(), moduleClassLoader);
            this.processViewAnnotations(ejbClass, (SessionBeanComponentDescription) componentDescription);
        }
    }

    /**
     * Processes the session bean for remote and local views and updates the {@link SessionBeanComponentDescription}
     * accordingly
     *
     * @param sessionBeanClass The bean class
     * @param sessionBeanComponentDescription
     *                         The component description
     * @throws DeploymentUnitProcessingException
     *
     */
    private void processViewAnnotations(Class<?> sessionBeanClass, final SessionBeanComponentDescription sessionBeanComponentDescription) throws DeploymentUnitProcessingException {
        final Collection<Class<?>> remoteBusinessInterfaces = this.getRemoteBusinessInterfaces(sessionBeanClass);
        if (remoteBusinessInterfaces != null && !remoteBusinessInterfaces.isEmpty()) {
            sessionBeanComponentDescription.addRemoteBusinessInterfaceViews(this.toString(remoteBusinessInterfaces));
        }

        // fetch the local business interfaces of the bean
        Collection<Class<?>> localBusinessInterfaces = this.getLocalBusinessInterfaces(sessionBeanClass);
        if (localBusinessInterfaces != null && !localBusinessInterfaces.isEmpty()) {
            sessionBeanComponentDescription.addLocalBusinessInterfaceViews(this.toString(localBusinessInterfaces));
        }

        if (hasNoInterfaceView(sessionBeanClass)) {
            sessionBeanComponentDescription.addNoInterfaceView();
        }

        // EJB 3.1 FR 4.9.7 & 4.9.8, if the bean exposes no views
        if (hasNoViews(sessionBeanComponentDescription)) {
            final Set<Class<?>> potentialBusinessInterfaces = getPotentialBusinessInterfaces(sessionBeanClass);
            if (potentialBusinessInterfaces.isEmpty()) {
                sessionBeanComponentDescription.addNoInterfaceView();
            } else if (potentialBusinessInterfaces.size() == 1) {
                sessionBeanComponentDescription.addLocalBusinessInterfaceViews(potentialBusinessInterfaces.iterator().next().getName());
            }
        }
    }

    private Collection<Class<?>> getRemoteBusinessInterfaces(Class<?> sessionBeanClass) throws DeploymentUnitProcessingException {
        final Remote remoteViewAnnotation = sessionBeanClass.getAnnotation(Remote.class);
        if (remoteViewAnnotation == null) {
            Collection<Class<?>> interfaces = getBusinessInterfacesFromInterfaceAnnotations(sessionBeanClass, Remote.class);
            if (!interfaces.isEmpty()) {
                return interfaces;
            }
            return Collections.emptySet();
        }
        Class<?>[] remoteViews = remoteViewAnnotation.value();
        if (remoteViews == null || remoteViews.length == 0) {
            Set<Class<?>> interfaces = getPotentialBusinessInterfaces(sessionBeanClass);
            if (interfaces.size() != 1)
                throw new DeploymentUnitProcessingException("Bean " + sessionBeanClass + " specifies @Remote annotation, but does not implement 1 interface");
            return interfaces;
        }
        return Arrays.asList(remoteViews);
    }

    private Collection<Class<?>> getLocalBusinessInterfaces(Class<?> sessionBeanClass) throws DeploymentUnitProcessingException {
        final Local localViewAnnotation = sessionBeanClass.getAnnotation(Local.class);
        if (localViewAnnotation == null) {
            Collection<Class<?>> interfaces = getBusinessInterfacesFromInterfaceAnnotations(sessionBeanClass, Local.class);
            if (!interfaces.isEmpty()) {
                return interfaces;
            }
            return Collections.emptySet();
        }
        Class<?>[] remoteViews = localViewAnnotation.value();
        if (remoteViews == null || remoteViews.length == 0) {
            Set<Class<?>> interfaces = getPotentialBusinessInterfaces(sessionBeanClass);
            if (interfaces.size() != 1)
                throw new DeploymentUnitProcessingException("Bean " + sessionBeanClass + " specifies @Remote annotation, but does not implement 1 interface");
            return interfaces;
        }
        return Arrays.asList(remoteViews);
    }

    private static Collection<Class<?>> getBusinessInterfacesFromInterfaceAnnotations(Class<?> sessionBeanClass, Class<? extends Annotation> annotation) {
        final Set<Class<?>> potentialBusinessInterfaces = getPotentialBusinessInterfaces(sessionBeanClass);
        final Set<Class<?>> businessInterfaces = new HashSet<Class<?>>();
        for (Class<?> iface : potentialBusinessInterfaces) {
            if (iface.getAnnotation(annotation) != null) {
                businessInterfaces.add(iface);
            }
        }
        return businessInterfaces;
    }

    /**
     * Returns all interfaces implemented by a bean that are eligible to be business interfaces
     *
     * @param sessionBeanClass The bean class
     * @return A collection of all potential business interfaces
     */
    private static Set<Class<?>> getPotentialBusinessInterfaces(Class<?> sessionBeanClass) {
        return getPotentialViewInterfaces(sessionBeanClass);
    }

    /**
     * Returns true if the <code>sessionBeanClass</code> has a {@link LocalBean no-interface view annotation}.
     * Else returns false.
     *
     * @param sessionBeanClass The session bean {@link Class class}
     * @return
     */
    private static boolean hasNoInterfaceView(Class<?> sessionBeanClass) {
        return sessionBeanClass.getAnnotation(LocalBean.class) != null;
    }

    private static boolean hasNoViews(SessionBeanComponentDescription sessionBeanComponentDescription) {
        return sessionBeanComponentDescription.getViews() == null || sessionBeanComponentDescription.getViews().isEmpty();
    }

    private Class<?> getEjbClass(String className, ClassLoader cl) throws DeploymentUnitProcessingException {
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Could not load EJB class " + className, e);
        }
    }

    private Collection<String> toString(Collection<Class<?>> classes) {
        final Collection<String> classNames = new ArrayList<String>(classes.size());
        for (Class<?> klass : classes) {
            classNames.add(klass.getName());
        }
        return classNames;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
