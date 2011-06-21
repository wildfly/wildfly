/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractAnnotationEJBProcessor<D extends EJBComponentDescription> extends AbstractComponentConfigProcessor {
    protected abstract Class<D> getComponentDescriptionType();

    protected abstract void processAnnotations(final ClassInfo beanClass, final CompositeIndex index, D componentDescription) throws DeploymentUnitProcessingException;

    @Override
    protected final void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final ClassInfo beanClass = index.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (beanClass == null) {
            return; // We can't continue without the annotation index info.
        }
        Class<D> componentDescriptionType = getComponentDescriptionType();
        // Only process EJB deployments and components that are applicable
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit) || !(componentDescriptionType.isAssignableFrom(componentDescription.getClass()))) {
            return;
        }
        processAnnotations(beanClass, index, componentDescriptionType.cast(componentDescription));
    }

    protected ClassInfo getSuperClass(final ClassInfo klass, final CompositeIndex index) {
        final DotName superClassDotName = klass.superName();
        if (superClassDotName == null) {
            return null;
        }
        return index.getClassByName(superClassDotName);
    }
}
