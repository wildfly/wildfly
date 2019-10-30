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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ManagedBean;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.ee.managedbean.component.ManagedBeanCreateInterceptor;
import org.jboss.as.ee.managedbean.component.ManagedBeanResourceReferenceProcessor;
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.AccessCheckingInterceptor;
import org.jboss.invocation.ContextClassLoaderInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

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
        final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final PropertyReplacer replacer = EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit);
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
                throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@ManagedBean", target);
            }
            final ClassInfo classInfo = (ClassInfo) target;
            // skip if it's not a valid managed bean class
            if (!assertManagedBeanClassValidity(classInfo)) {
                continue;
            }
            final String beanClassName = classInfo.name().toString();

            // Get the managed bean name from the annotation
            final AnnotationValue nameValue = instance.value();
            final String beanName = (nameValue == null || nameValue.asString().isEmpty()) ? beanClassName : replacer.replaceProperties(nameValue.asString());
            final ManagedBeanComponentDescription componentDescription = new ManagedBeanComponentDescription(beanName, beanClassName, moduleDescription, deploymentUnit.getServiceName());

            // Add the view
            ViewDescription viewDescription = new ViewDescription(componentDescription, beanClassName);
            viewDescription.getConfigurators().addFirst(new ViewConfigurator() {
                public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    // Add MB association interceptors
                    configuration.addClientPostConstructInterceptor(ManagedBeanCreateInterceptor.FACTORY, InterceptorOrder.ClientPostConstruct.INSTANCE_CREATE);
                    final ClassLoader classLoader = componentConfiguration.getModuleClassLoader();
                    configuration.addViewInterceptor(AccessCheckingInterceptor.getFactory(), InterceptorOrder.View.CHECKING_INTERCEPTOR);
                    configuration.addViewInterceptor(new ImmediateInterceptorFactory(new ContextClassLoaderInterceptor(classLoader)), InterceptorOrder.View.TCCL_INTERCEPTOR);
                }
            });
            viewDescription.getBindingNames().addAll(Arrays.asList("java:module/" + beanName, "java:app/" + moduleDescription.getModuleName() + "/" + beanName));
            componentDescription.getViews().add(viewDescription);
            moduleDescription.addComponent(componentDescription);

            // register an EEResourceReferenceProcessor which can process @Resource references to this managed bean.
            registry.registerResourceReferenceProcessor(new ManagedBeanResourceReferenceProcessor(beanClassName));
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Returns true if the passed <code>managedBeanClass</code> meets the requirements set by the Managed bean spec about
     * bean implementation classes. The passed <code>managedBeanClass</code> must not be an interface and must not be final or abstract.
     * If it passes these requirements then this method returns true. Else it returns false.
     *
     * @param managedBeanClass The session bean class
     * @return
     */
    private static boolean assertManagedBeanClassValidity(final ClassInfo managedBeanClass) {
        final short flags = managedBeanClass.flags();
        final String className = managedBeanClass.name().toString();
        // must *not* be an interface
        if (Modifier.isInterface(flags)) {
            ROOT_LOGGER.invalidManagedBeanInterface("MB.2.1.1", className);
            return false;
        }
        // bean class must *not* be abstract or final
        if (Modifier.isAbstract(flags) || Modifier.isFinal(flags)) {
            ROOT_LOGGER.invalidManagedBeanAbstractOrFinal("MB.2.1.1", className);
            return false;
        }
        // valid class
        return true;
    }
}
