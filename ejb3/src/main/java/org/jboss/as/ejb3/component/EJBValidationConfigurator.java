/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * Configurator that validates that a Jakarta Enterprise Beans class does not violate the Jakarta Enterprise Beans specification.
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
                break;
            }
        }

        EjbValidationsUtil.verifyEjbClassAndDefaultConstructor(ctor, configuration.getComponentClass().getEnclosingClass(), noInterface, description.getComponentName(), description.getComponentClassName(), configuration.getComponentClass().getModifiers());
        EjbValidationsUtil.verifyEjbPublicMethodAreNotFinalNorStatic(configuration.getComponentClass().getMethods(),description.getComponentClassName());
        for ( Class<?> interfaceClass : configuration.getComponentClass().getInterfaces())
            EjbValidationsUtil.verifyEjbPublicMethodAreNotFinalNorStatic(interfaceClass.getMethods(), interfaceClass.getCanonicalName());
    }
}
