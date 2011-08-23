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

package org.jboss.as.ejb3.injection;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import javax.ejb.EJB;

import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

/**
 * Resolves JNDI name for @EJB annotated methods & fields.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EjbResourceResolver {

    private static final EjbResourceResolver SINGLETON = new EjbResourceResolver();

    private EjbResourceResolver() {}

    public static EjbResourceResolver getInstance() {
        return SINGLETON;
    }

    public String resolve(final DeploymentUnit unit, final Field field) {
        final EJB annotation = field.getAnnotation(EJB.class);
        final String beanName = annotation.beanName();
        final String beanType = getInterfaceType(field, annotation);
        return resolve(beanName, beanType, unit);
    }

    public String resolve(final DeploymentUnit unit, final Method method) {
        final EJB annotation = method.getAnnotation(EJB.class);
        final String beanName = annotation.beanName();
        final String beanType = getInterfaceType(method, annotation);
        return resolve(beanName, beanType, unit);
    }

    private String getInterfaceType(final Field field, final EJB annotation) {
        if (annotation.beanInterface() == null || annotation.beanInterface().equals(Object.class)) {
            return field.getType().getName();
        } else {
            return annotation.beanInterface().getName();
        }
    }

    private String getInterfaceType(final Method method, final EJB annotation) {
        if (annotation.beanInterface() == null || annotation.beanInterface().equals(Object.class)) {
            return method.getParameterTypes()[0].getName();
        } else {
            return annotation.beanInterface().getName();
        }
    }

    private String resolve(final String beanName, final String typeName, final DeploymentUnit unit) {
        // TODO: copied from EjbInjectionSource - provide utility class for it ???
        final Set<ViewDescription> componentsForViewName = getViews(beanName, typeName, unit);
        if (componentsForViewName.isEmpty()) {
            throw new RuntimeException("No component found for type '" + typeName + "' with name " + beanName);
        }
        if (componentsForViewName.size() > 1) {
            throw new RuntimeException("More than 1 component found for type '" + typeName + "' and bean name " + beanName);
        }
        final ViewDescription description = componentsForViewName.iterator().next();
        final String ejbName = description.getComponentDescription().getComponentName();
        final String applicationName = getEarName(unit);
        final String moduleName = description.getComponentDescription().getModuleName();
        // TODO: copied from EjbJndiBindingsDeploymentUnitProcessor - provide utility class for it ???
        final String globalJNDIBaseName = "java:global/" + (applicationName != null ? applicationName + "/" : "") + moduleName + "/" + ejbName;
        final String viewClassName = description.getViewClassName();
        return globalJNDIBaseName + "!" + viewClassName;
    }

    private Set<ViewDescription> getViews(final String beanName, final String typeName, final DeploymentUnit unit) {
        final EEApplicationDescription applicationDescription = unit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Set<ViewDescription> componentsForViewName;
        if (beanName != null && !"".equals(beanName)) {
            componentsForViewName = applicationDescription.getComponents(beanName, typeName, deploymentRoot.getRoot());
        } else {
            componentsForViewName = applicationDescription.getComponentsForViewName(typeName);
        }
        return componentsForViewName;
    }

    // TODO: copied from EjbJndiBindingsDeploymentUnitProcessor - provide utility class for it ???
    private String getEarName(DeploymentUnit deploymentUnit) {
        DeploymentUnit parentDU = deploymentUnit.getParent();
        if (parentDU == null) {
            String duName = deploymentUnit.getName();
            if (duName.endsWith(".ear")) {
                return duName.substring(0, duName.length() - ".ear".length());
            }
            return null;
        }
        // traverse to top level DU
        while (parentDU.getParent() != null) {
            parentDU = parentDU.getParent();
        }
        String duName = parentDU.getName();
        if (duName.endsWith(".ear")) {
            return duName.substring(0, duName.length() - ".ear".length());
        }
        return null;
    }

}
