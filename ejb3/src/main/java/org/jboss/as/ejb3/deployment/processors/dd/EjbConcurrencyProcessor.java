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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.modules.Module;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DUP that processes concurrency information for singleton beans.
 * <p/>
 * It checks to make sure that concurrency annotations have not been overriden by a sub class,
 * and then parses ejb-jar.xml to read concurrency information from the descriptor.
 * We cannot check if methods have been overriden until we can actually load the class.
 * <p/>
 * from ejb-jar.xml.
 *
 * @author Stuart Douglas
 */
public class EjbConcurrencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        if(eeModuleDescription == null) {
            return;
        }

        for (ComponentDescription description : eeModuleDescription.getComponentDescriptions()) {
            if (description instanceof SingletonComponentDescription || description instanceof StatefulComponentDescription) {
                try {
                    Class<?> componentClass = module.getClassLoader().loadClass(description.getComponentClassName());
                    checkMethodOverrides(componentClass, (SessionBeanComponentDescription) description, index);

                    if (ejbJarMetaData != null) {
                        EnterpriseBeanMetaData bean = ejbJarMetaData.getEnterpriseBean(((SessionBeanComponentDescription)description).getEJBName());
                        if (bean instanceof SessionBean31MetaData) {
                            processBean((SessionBean31MetaData) bean, (SessionBeanComponentDescription) description, index, componentClass);
                        }
                    }

                } catch (ClassNotFoundException e) {
                    throw new DeploymentUnitProcessingException("Could not load EJB class " + description.getComponentClassName(), e);
                }
            }
        }

    }

    /**
     * Removes annotation information from a class if the method has been overridden with a method that has no annotation,
     * as the jandex index cannot be used to get information about methods with no annotations
     */
    private void checkMethodOverrides(final Class<?> componentClass, final SessionBeanComponentDescription description, final DeploymentReflectionIndex index) {

        ClassReflectionIndex<?> classIndex = index.getClassIndex(componentClass);
        Iterator<Map.Entry<MethodIdentifier, LockType>> iterator = description.getMethodApplicableLockTypes().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MethodIdentifier, LockType> entry = iterator.next();
            if (annotationOverridden(classIndex, index, entry.getKey(), Lock.class)) {
                iterator.remove();
            }
        }
        Iterator<Map.Entry<MethodIdentifier, AccessTimeout>> iterator2 = description.getMethodApplicableAccessTimeouts().entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<MethodIdentifier, AccessTimeout> entry = iterator2.next();
            if (annotationOverridden(classIndex, index, entry.getKey(), AccessTimeout.class)) {
                iterator.remove();
            }
        }

    }

    private boolean annotationOverridden(final ClassReflectionIndex<?> classIndex, final DeploymentReflectionIndex index, final MethodIdentifier method, final Class<? extends Annotation> annotation) {
        ClassReflectionIndex<?> cindex = classIndex;
        while (cindex != null && cindex.getIndexedClass() != Object.class) {
            Method m = cindex.getMethod(method);
            if (m != null) {
                return !m.isAnnotationPresent(annotation);
            }
            cindex = index.getClassIndex(cindex.getIndexedClass().getSuperclass());
        }
        return false;
    }


    /**
     * Processes method level concurrency from the deployment descriptor.
     */
    private void processBean(SessionBean31MetaData singletonBeanMetaData, SessionBeanComponentDescription singletonComponentDescription, DeploymentReflectionIndex reflectionIndex, Class<?> componentClass) throws DeploymentUnitProcessingException {

        // add method level lock type to the description
        ConcurrentMethodsMetaData concurrentMethods = singletonBeanMetaData.getConcurrentMethods();
        if (concurrentMethods != null) {
            for (ConcurrentMethodMetaData concurrentMethod : concurrentMethods) {
                LockType methodLockType = concurrentMethod.getLockType();
                Method method = resolveMethod(reflectionIndex, (Class<Object>) componentClass, concurrentMethod.getMethod());
                final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
                if(methodLockType != null) {
                    singletonComponentDescription.setLockType(methodLockType, methodIdentifier);
                }
                final AccessTimeoutMetaData accessTimeout = concurrentMethod.getAccessTimeout();
                if(accessTimeout != null) {
                    singletonComponentDescription.setAccessTimeout(new AccessTimeout() {
                        @Override
                        public long value() {
                            return accessTimeout.getTimeout();
                        }

                        @Override
                        public TimeUnit unit() {
                            return accessTimeout.getUnit();
                        }

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return AccessTimeout.class;
                        }
                    }, methodIdentifier);
                }
            }
        }

    }

    private Method resolveMethod(final DeploymentReflectionIndex index, final Class<Object> componentClass, final NamedMethodMetaData methodData) throws DeploymentUnitProcessingException {
        if(componentClass == null) {
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
