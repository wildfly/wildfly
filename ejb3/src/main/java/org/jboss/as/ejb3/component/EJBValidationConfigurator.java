/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

/**
 *
 * Configurator that validates than an EJB class does not validate the EJB specification
 *
 * @author Stuart Douglas
 */
public class EJBValidationConfigurator implements ComponentConfigurator {

    public static final EJBValidationConfigurator INSTANCE = new EJBValidationConfigurator();

    private EJBValidationConfigurator() {

    }

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
        final DeploymentReflectionIndex deploymentReflectionIndex = context.getDeploymentUnit().getAttachment(Attachments.REFLECTION_INDEX);
        final ClassReflectionIndex<?> classIndex = deploymentReflectionIndex.getClassIndex(configuration.getComponentClass());
        final Constructor<?> ctor = classIndex.getConstructor(new String[0]);

        if(ctor == null) {
            throw EjbMessages.MESSAGES.ejbMustHavePublicDefaultConstructor(description.getComponentName(), description.getComponentClassName());
        }
        if(!Modifier.isPublic(ctor.getModifiers())) {
            throw EjbMessages.MESSAGES.ejbMustHavePublicDefaultConstructor(description.getComponentName(), description.getComponentClassName());
        }
        if(configuration.getComponentClass().getEnclosingClass() != null) {
            throw EjbMessages.MESSAGES.ejbMustNotBeInnerClass(description.getComponentName(), description.getComponentClassName());
        }
        if(!Modifier.isPublic(configuration.getComponentClass().getModifiers())) {
            throw EjbMessages.MESSAGES.ejbMustBePublicClass(description.getComponentName(), description.getComponentClassName());
        }
        if(Modifier.isFinal(configuration.getComponentClass().getModifiers())) {
            throw EjbMessages.MESSAGES.ejbMustNotBeFinalClass(description.getComponentName(), description.getComponentClassName());
        }
    }
}
