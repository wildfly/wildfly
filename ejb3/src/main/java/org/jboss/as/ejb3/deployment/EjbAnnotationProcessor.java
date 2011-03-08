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

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
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
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jaikiran Pai
 */
public class EjbAnnotationProcessor implements DeploymentUnitProcessor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(EjbAnnotationProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get hold of the deployment unit
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getAppName();
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Skipping EJB annotation processing since no composite annotation index found in unit: " + deploymentUnit);
            }
            return;
        }
        // Find any session bean annotations
        final List<AnnotationInstance> sessionBeanAnnotations = this.getSessionBeans(compositeIndex);
        if (sessionBeanAnnotations == null || sessionBeanAnnotations.isEmpty()) {
            if (logger.isTraceEnabled()) {
                logger.trace("No session bean annotations found in unit: " + deploymentUnit);
            }
            return;
        }
        // Mark it as a EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);

        // process these session bean annotations and create component descriptions out of it
        for (AnnotationInstance sessionBeanAnnotation : sessionBeanAnnotations) {
            AnnotationTarget target = sessionBeanAnnotation.target();
            if (!(target instanceof ClassInfo)) {
                // Let's just WARN and move on. No need to throw an error
                logger.warn(sessionBeanAnnotation.name() + " annotation is expected to be only on classes. " + target + " is not a class");
                continue;
            }
            final ClassInfo classInfo = (ClassInfo) target;
            final String beanClassName = classInfo.name().local();

            // Get the bean name from the annotation
            final AnnotationValue nameValue = sessionBeanAnnotation.value();
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? beanClassName : nameValue.asString();
            SessionBeanComponentDescription sessionBeanDescription = null;
            String annotation = sessionBeanAnnotation.name().toString();
            if (Stateless.class.getName().equals(annotation)) {
                sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
            } else if (Stateful.class.getName().equals(annotation)) {
                sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
            } else if (Singleton.class.getName().equals(annotation)) {
                // TODO: We might need a SingletonComponentDescription. This is just temporary for now
                sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
            } else {
                throw new IllegalArgumentException("Unknown session bean type: " + annotation);
            }
            // Add this component description to the module description
            moduleDescription.addComponent(sessionBeanDescription);

            //final ServiceName baseName = deploymentUnit.getServiceName().append("component").append(beanName);

            // TODO: Add the bean views
            //slsbDescription.getViewClassNames().add(beanClassName);

            // TODO: Bind the view(s) to its two JNDI locations
//            final BindingDescription moduleBinding = new BindingDescription();
//            moduleBinding.setAbsoluteBinding(true);
//            moduleBinding.setBindingName("java:module/" + beanName);
//            moduleBinding.setBindingType(beanClassName);
//            moduleBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseName.append("VIEW").append(beanClassName)));
//            slsbDescription.getBindings().add(moduleBinding);
//            final BindingDescription appBinding = new BindingDescription();
//            appBinding.setAbsoluteBinding(true);
//            appBinding.setBindingName("java:app/" + moduleDescription.getModuleName() + "/" + beanName);
//            appBinding.setBindingType(beanClassName);
//            appBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseName.append("VIEW").append(beanClassName)));
//            slsbDescription.getBindings().add(appBinding);
//            moduleDescription.addComponent(slsbDescription);
        }
    }

    /**
     * Returns a list of session bean annotations (@Stateless, @Stateful and @Singleton)
     * <p/>
     * Returns an empty list if no session bean annotations were found
     *
     * @param index The composite annotation index
     * @return
     */
    private List<AnnotationInstance> getSessionBeans(CompositeIndex index) {
        List<AnnotationInstance> sessionBeans = new ArrayList<AnnotationInstance>();
        // @Stateless
        List<AnnotationInstance> slsbs = index.getAnnotations(DotName.createSimple(Stateless.class.getName()));
        if (slsbs != null) {
            sessionBeans.addAll(slsbs);
        }
        // @Stateful
        List<AnnotationInstance> sfsbs = index.getAnnotations(DotName.createSimple(Stateful.class.getName()));
        if (sfsbs != null) {
            sessionBeans.addAll(sfsbs);
        }
        // @Singleton
        List<AnnotationInstance> singletons = index.getAnnotations(DotName.createSimple(Singleton.class.getName()));
        if (singletons != null) {
            sessionBeans.addAll(singletons);
        }
        return sessionBeans;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
