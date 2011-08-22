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
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.Module;

import javax.ejb.Lock;
import javax.ejb.LockType;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class that can merge {@link javax.ejb.Lock} metadata
 *
 * @author Stuart Douglas
 */
public class EjbLockMergingProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);


        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (ComponentDescription componentConfiguration : componentConfigurations) {
            if (componentConfiguration instanceof SessionBeanComponentDescription) {
                processComponentConfig(applicationClasses, module, deploymentReflectionIndex, (SessionBeanComponentDescription) componentConfiguration);
            }
        }
    }

    private void processComponentConfig(final EEApplicationClasses applicationClasses, final Module module, final DeploymentReflectionIndex deploymentReflectionIndex, final SessionBeanComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {

        final Class<?> componentClass;
        try {
            componentClass = module.getClassLoader().loadClass(componentConfiguration.getEJBClassName());
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Could not load EJB class " + componentConfiguration.getEJBClassName(), e);
        }

        handleAnnotations(applicationClasses, deploymentReflectionIndex, componentClass, componentConfiguration);
        handleDeploymentDescriptor(deploymentReflectionIndex, componentClass, componentConfiguration);
    }

    private void handleAnnotations(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<LockType> annotationData = MethodAnnotationAggregator.<Lock, LockType>runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Lock.class);
        for (Map.Entry<String, List<LockType>> entry : annotationData.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setBeanLevelLockType(entry.getKey(), entry.getValue().get(0));
            }
        }
        for (Map.Entry<Method, List<LockType>> entry : annotationData.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setLockType(entry.getValue().get(0), MethodIdentifier.getIdentifierForMethod(entry.getKey()));
            }
        }
    }

    private void handleDeploymentDescriptor(final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getDescriptorData() == null) {
            return;
        }
        SessionBeanMetaData sessionBeanMetaData = componentConfiguration.getDescriptorData();
        if (sessionBeanMetaData instanceof SessionBean31MetaData) {
            SessionBean31MetaData descriptor = (SessionBean31MetaData) sessionBeanMetaData;

            if (descriptor.getLockType() != null) {
                componentConfiguration.setBeanLevelLockType(componentConfiguration.getEJBClassName(), descriptor.getLockType());
            }

            final ConcurrentMethodsMetaData methods = descriptor.getConcurrentMethods();
            if (methods != null) {
                for (final ConcurrentMethodMetaData method : methods) {
                    if (method.getLockType() != null) {
                        final Method realMethod = resolveMethod(deploymentReflectionIndex, componentClass, method.getMethod());
                        componentConfiguration.setLockType(method.getLockType(), MethodIdentifier.getIdentifierForMethod(realMethod));
                    }

                }
            }
        }
    }


    private Method resolveMethod(final DeploymentReflectionIndex index, final Class<?> componentClass, final NamedMethodMetaData methodData) throws DeploymentUnitProcessingException {
        if (componentClass == null) {
            throw new DeploymentUnitProcessingException("Could not find method" + methodData.getMethodName() + "with parameter types" + methodData.getMethodParams() + " referenced in ejb-jar.xml");
        }
        final ClassReflectionIndex<?> classIndex = index.getClassIndex(componentClass);

        if (methodData.getMethodParams() == null) {
            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName());
            if (methods.isEmpty()) {
                return resolveMethod(index, (Class<Object>) componentClass.getSuperclass(), methodData);
            } else if (methods.size() > 1) {
                throw new DeploymentUnitProcessingException("More than one method " + methodData.getMethodName() + "found on class" + componentClass.getName() + " referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity");
            }
            return methods.iterator().next();
        } else {
            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName(), methodData.getMethodParams().size());
            for (final Method method : methods) {
                boolean match = true;
                for (int i = 0; i < method.getParameterTypes().length; ++i) {
                    if (!method.getParameterTypes()[i].getName().equals(methodData.getMethodParams().get(i))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return method;
                }
            }
        }
        return resolveMethod(index, (Class<Object>) componentClass.getSuperclass(), methodData);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
