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

package org.jboss.as.ee.managedbean.processors;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.EEResourceReferenceProcessorRegistry;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.managedbean.component.ManagedBeanAssociatingInterceptorFactory;
import org.jboss.as.ee.managedbean.component.ManagedBeanCreateInterceptorFactory;
import org.jboss.as.ee.managedbean.component.ManagedBeanDestroyInterceptorFactory;
import org.jboss.as.ee.managedbean.component.ManagedBeanResourceReferenceProcessor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import javax.annotation.ManagedBean;
import java.util.Arrays;
import java.util.List;

/**
 * Deployment unit processor responsible for scanning a deployment to find classes with {@code javax.annotation.ManagedBean} annotations.
 * Note:  This processor only supports JSR-316 compliant managed beans.  So it will not handle complimentary spec additions (ex. EJB).
 *
 * @author John E. Bailey
 */
public class ManagedBeanAnnotationProcessor implements DeploymentUnitProcessor {

    static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());

    /**
     * Check the deployment annotation index for all classes with the @ManagedBean annotation.  For each class with the
     * annotation, collect all the required information to create a managed bean instance, and attach it to the context.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            return;
        }
        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return;
        }

        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (!(target instanceof ClassInfo)) {
                throw new DeploymentUnitProcessingException("The ManagedBean annotation is only allowed at the class level: " + target);
            }
            final ClassInfo classInfo = (ClassInfo) target;
            final String beanClassName = classInfo.name().toString();

            // Get the managed bean name from the annotation
            final AnnotationValue nameValue = instance.value();
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? beanClassName : nameValue.asString();
            final ComponentDescription componentDescription = new ComponentDescription(beanName, beanClassName, moduleDescription, applicationClasses.getOrAddClassByName(beanClassName), deploymentUnit.getServiceName(), applicationClasses);

            // Add the view
            ViewDescription viewDescription = new ViewDescription(componentDescription, beanClassName);
            viewDescription.getConfigurators().addFirst(new ViewConfigurator() {
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    // Add MB association interceptors
                    final Object contextKey = new Object();
                    configuration.addViewPostConstructInterceptor(new ManagedBeanCreateInterceptorFactory(contextKey), InterceptorOrder.ViewPostConstruct.INSTANCE_CREATE);
                    final ManagedBeanAssociatingInterceptorFactory associatingInterceptorFactory = new ManagedBeanAssociatingInterceptorFactory(contextKey);
                    configuration.addViewInterceptor(associatingInterceptorFactory, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
                    configuration.addViewPreDestroyInterceptor(new ManagedBeanDestroyInterceptorFactory(contextKey), InterceptorOrder.ViewPreDestroy.INSTANCE_DESTROY);
                }
            });
            viewDescription.getBindingNames().addAll(Arrays.asList("java:module/" + beanName, "java:app/" + moduleDescription.getModuleName() + "/" + beanName));
            componentDescription.getViews().add(viewDescription);
            moduleDescription.addComponent(componentDescription);

            // register a EEResourceReferenceProcessor which can process @Resource references to this managed bean.
            EEResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(new ManagedBeanResourceReferenceProcessor(beanClassName));
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
