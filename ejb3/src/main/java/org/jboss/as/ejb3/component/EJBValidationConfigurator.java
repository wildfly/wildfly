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

import static org.jboss.as.ejb3.util.MethodInfoHelper.EMPTY_STRING_ARRAY;

import java.lang.reflect.Constructor;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.util.EjbValidationsUtil;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;

/**
 *
 * Configurator that validates than an Jakarta Enterprise Beans class does not validate the Jakarta Enterprise Beans specification
 *
 * @author Stuart Douglas
 */
public class EJBValidationConfigurator implements ComponentConfigurator {

    public static final EJBValidationConfigurator INSTANCE = new EJBValidationConfigurator();

    private EJBValidationConfigurator() {

    }

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
        final ClassReflectionIndex classIndex = context.getDeploymentUnit().getAttachment(Attachments.REFLECTION_INDEX).getClassIndex(configuration.getComponentClass());
        final Constructor<?> ctor = classIndex.getConstructor(EMPTY_STRING_ARRAY);
        boolean noInterface = false;
        for(ViewDescription view : description.getViews()) {
            if(view.getViewClassName().equals(description.getComponentClassName())) {
                noInterface = true;
            }
        }

        EjbValidationsUtil.getBusinessMethods(configuration.getComponentClass());

        EjbValidationsUtil.verifyEjbClassAndDefaultConstructor(ctor, configuration.getComponentClass().getEnclosingClass(), noInterface, description.getComponentName(), description.getComponentClassName(), configuration.getComponentClass().getModifiers());
        EjbValidationsUtil.verifyEjbPublicMethodAreNotFinalNorStatic(configuration.getComponentClass().getDeclaredMethods(),description.getComponentClassName());
        for ( Class<?> interfaceClass : configuration.getComponentClass().getInterfaces())
            EjbValidationsUtil.verifyEjbPublicMethodAreNotFinalNorStatic(interfaceClass.getDeclaredMethods(), interfaceClass.getCanonicalName());
    }
}
