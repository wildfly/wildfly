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

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AroundInvokeMetaData;
import org.jboss.metadata.ejb.spec.AroundInvokesMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;
import org.jboss.modules.Module;

/**
 * Deployment descriptor that resolves interceptor methods defined in ejb-jar.xml that could not be resolved at
 * DD parse time.
 *
 * @author Stuart Douglas
 */
public class DeploymentDescriptorMethodProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (eeModuleDescription != null) {
            for (final ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
                if (component instanceof EJBComponentDescription) {
                    try {
                        if (component instanceof SessionBeanComponentDescription || component instanceof MessageDrivenComponentDescription)
                            handleSessionBean((EJBComponentDescription) component, module, reflectionIndex);
                        if (component instanceof StatelessComponentDescription || component instanceof MessageDrivenComponentDescription) {
                            handleStatelessSessionBean((EJBComponentDescription) component, module, reflectionIndex);
                        }
                    } catch (ClassNotFoundException e) {
                        throw EjbLogger.ROOT_LOGGER.failToLoadComponentClass(e, component.getComponentName());
                    }
                }
            }
        }


    }

    /**
     * Handles setting up the ejbCreate and ejbRemove methods  for stateless session beans and MDB's
     *
     * @param component       The component
     * @param module      The module
     * @param reflectionIndex The reflection index
     */
    private void handleStatelessSessionBean(final EJBComponentDescription component, final Module module, final DeploymentReflectionIndex reflectionIndex) throws ClassNotFoundException, DeploymentUnitProcessingException {

        final Class<?> componentClass = ClassLoadingUtils.loadClass(component.getComponentClassName(), module);
        final MethodIdentifier ejbCreateId = MethodIdentifier.getIdentifier(void.class, "ejbCreate");
        final Method ejbCreate = ClassReflectionIndexUtil.findMethod(reflectionIndex, componentClass, ejbCreateId);
        if (ejbCreate != null) {
            final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
            builder.setPostConstruct(ejbCreateId);
            component.addInterceptorMethodOverride(ejbCreate.getDeclaringClass().getName(), builder.build());
        }
        final MethodIdentifier ejbRemoveId = MethodIdentifier.getIdentifier(void.class, "ejbRemove");
        final Method ejbRemove = ClassReflectionIndexUtil.findMethod(reflectionIndex, componentClass, ejbRemoveId);
        if (ejbRemove != null) {
            final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
            builder.setPreDestroy(ejbRemoveId);
            component.addInterceptorMethodOverride(ejbRemove.getDeclaringClass().getName(), builder.build());
        }
    }

    private void handleSessionBean(final EJBComponentDescription component, final Module module, final DeploymentReflectionIndex reflectionIndex) throws ClassNotFoundException, DeploymentUnitProcessingException {

        if (component.getDescriptorData() == null) {
            return;
        }
        final Class<?> componentClass = ClassLoadingUtils.loadClass(component.getComponentClassName(), module);

        final EnterpriseBeanMetaData metaData = component.getDescriptorData();

        AroundInvokesMetaData aroundInvokes = null;
        if (metaData instanceof SessionBeanMetaData) {
            aroundInvokes = ((SessionBeanMetaData) metaData).getAroundInvokes();
        } else if (metaData instanceof MessageDrivenBeanMetaData) {
            aroundInvokes = ((MessageDrivenBeanMetaData) metaData).getAroundInvokes();
        }

        if (aroundInvokes != null) {
            for (AroundInvokeMetaData aroundInvoke : aroundInvokes) {
                final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                String methodName = aroundInvoke.getMethodName();
                MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Object.class, methodName, InvocationContext.class);
                builder.setAroundInvoke(methodIdentifier);
                if (aroundInvoke.getClassName() == null || aroundInvoke.getClassName().isEmpty()) {
                    final String className = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, componentClass, methodIdentifier).getDeclaringClass().getName();
                    component.addInterceptorMethodOverride(className, builder.build());
                } else {
                    component.addInterceptorMethodOverride(aroundInvoke.getClassName(), builder.build());
                }
            }
        }

        // post-construct(s) of the interceptor configured (if any) in the deployment descriptor
        LifecycleCallbacksMetaData postConstructs = metaData.getPostConstructs();
        if (postConstructs != null) {
            for (LifecycleCallbackMetaData postConstruct : postConstructs) {
                final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                String methodName = postConstruct.getMethodName();
                MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                builder.setPostConstruct(methodIdentifier);
                if (postConstruct.getClassName() == null || postConstruct.getClassName().isEmpty()) {
                    final String className = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, componentClass, methodIdentifier).getDeclaringClass().getName();
                    component.addInterceptorMethodOverride(className, builder.build());
                } else {
                    component.addInterceptorMethodOverride(postConstruct.getClassName(), builder.build());
                }
            }
        }

        // pre-destroy(s) of the interceptor configured (if any) in the deployment descriptor
        final LifecycleCallbacksMetaData preDestroys = metaData.getPreDestroys();
        if (preDestroys != null) {
            for (final LifecycleCallbackMetaData preDestroy : preDestroys) {
                final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                final String methodName = preDestroy.getMethodName();
                final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                builder.setPreDestroy(methodIdentifier);
                if (preDestroy.getClassName() == null || preDestroy.getClassName().isEmpty()) {
                    final String className = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, componentClass, methodIdentifier).getDeclaringClass().getName();
                    component.addInterceptorMethodOverride(className, builder.build());
                } else {
                    component.addInterceptorMethodOverride(preDestroy.getClassName(), builder.build());
                }
            }
        }

        if (component.isStateful()) {

            final SessionBeanMetaData sessionBeanMetadata = (SessionBeanMetaData) metaData;
            // pre-passivate(s) of the interceptor configured (if any) in the deployment descriptor
            final LifecycleCallbacksMetaData prePassivates = sessionBeanMetadata.getPrePassivates();
            if (prePassivates != null) {
                for (final LifecycleCallbackMetaData prePassivate : prePassivates) {
                    final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                    final String methodName = prePassivate.getMethodName();
                    final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                    builder.setPrePassivate(methodIdentifier);
                    if (prePassivate.getClassName() == null || prePassivate.getClassName().isEmpty()) {
                        final String className = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, componentClass, methodIdentifier).getDeclaringClass().getName();
                        component.addInterceptorMethodOverride(className, builder.build());
                    } else {
                        component.addInterceptorMethodOverride(prePassivate.getClassName(), builder.build());
                    }
                }
            }

            final LifecycleCallbacksMetaData postActivates = sessionBeanMetadata.getPostActivates();
            if (postActivates != null) {
                for (final LifecycleCallbackMetaData postActivate : postActivates) {
                    final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                    final String methodName = postActivate.getMethodName();
                    final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName);
                    builder.setPostActivate(methodIdentifier);
                    if (postActivate.getClassName() == null || postActivate.getClassName().isEmpty()) {
                        final String className = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, componentClass, methodIdentifier).getDeclaringClass().getName();
                        component.addInterceptorMethodOverride(className, builder.build());
                    } else {
                        component.addInterceptorMethodOverride(postActivate.getClassName(), builder.build());
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
