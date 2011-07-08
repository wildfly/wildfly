/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.modules.Module;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

/**
 * Deployment descriptor that resolves interceptor methods definined in ejb-jar.xml that could not be resolved at
 * DD parse time.
 *
 * @author Stuart Douglas
 */
public class DeploymentDescriptorMethodProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentReflectionIndex index = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        for (final ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            if (component instanceof EJBComponentDescription) {
                final EJBComponentDescription ejb = (EJBComponentDescription) component;
                if (!ejb.getAroundInvokeDDMethods().isEmpty() || !ejb.getPostConstructDDMethods().isEmpty() || !ejb.getPreDestroyDDMethods().isEmpty()) {
                    try {
                        final Class<?> clazz = module.getClassLoader().loadClass(ejb.getComponentClassName());
                        for (String aroundInvoke : ejb.getAroundInvokeDDMethods()) {
                            final MethodIdentifier aroundInvokeIdentifier = MethodIdentifier.getIdentifier(Object.class, aroundInvoke, InvocationContext.class);
                            Method method = ClassReflectionIndexUtil.findRequiredMethod(index, index.getClassIndex(clazz), aroundInvokeIdentifier);
                            applicationClassesDescription.getOrAddClassByName(method.getDeclaringClass().getName()).setAroundInvokeMethod(aroundInvokeIdentifier);
                        }
                        for (String preDestroy : ejb.getPreDestroyDDMethods()) {
                            final MethodIdentifier preDestroyIdentifier = MethodIdentifier.getIdentifier(void.class, preDestroy);
                            final Method method = ClassReflectionIndexUtil.findRequiredMethod(index, index.getClassIndex(clazz), preDestroyIdentifier);
                            applicationClassesDescription.getOrAddClassByName(method.getDeclaringClass().getName()).setPreDestroyMethod(preDestroyIdentifier);
                        }
                        for (String postConstruct : ejb.getPostConstructDDMethods()) {
                            final MethodIdentifier postConstructIdentifier = MethodIdentifier.getIdentifier(void.class, postConstruct);
                            final Method method = ClassReflectionIndexUtil.findRequiredMethod(index, index.getClassIndex(clazz), postConstructIdentifier);
                            applicationClassesDescription.getOrAddClassByName(method.getDeclaringClass().getName()).setPostConstructMethod(postConstructIdentifier);
                        }

                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException("Could not load component class " + ejb.getComponentClassName());
                    }

                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
