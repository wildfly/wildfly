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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.ServiceBindingSourceDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.LocalBean;
import java.util.List;
import java.util.Map;

/**
 * Processes {@link javax.ejb.LocalBean} annotation on session beans and creates
 * jndi binding for the the no-interface view.
 *
 * @author Jaikiran Pai
 */
public class NoInterfaceViewAnnotationProcessor extends AbstractComponentConfigProcessor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(NoInterfaceViewAnnotationProcessor.class);

    @Override
    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, CompositeIndex compositeIndex, AbstractComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final ClassInfo sessionBeanClass = compositeIndex.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (sessionBeanClass == null) {
            return; // We can't continue without the annotation index info.
        }
        // Only process EJB deployments
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit) || !(componentDescription instanceof SessionBeanComponentDescription)) {
            return;
        }
        SessionBeanComponentDescription sessionBeanComponentDescription = (SessionBeanComponentDescription) componentDescription;
        // TODO: We should only pick up EJB3.1 deployments for @LocalBean processing
        if (!this.hasNoInterfaceView(sessionBeanClass)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Session bean: " + sessionBeanComponentDescription.getEJBName() + " doesn't have a no-interface view");
            }
            return;
        }
        // add the no-interface view to the component description
        sessionBeanComponentDescription.addNoInterfaceView();

        // create the jndi bindings for the no-interface view

        // In case of EJB bindings, appname == .ear file name (if it's an .ear deployment)
        String applicationName = this.getEarName(deploymentUnit);
        String globalJNDIBaseName = "java:global/" + (applicationName != null ? applicationName + "/" : "") + sessionBeanComponentDescription.getModuleName() + "/" + sessionBeanComponentDescription.getEJBName();
        String appJNDIBaseName = "java:app/" + sessionBeanComponentDescription.getModuleName() + "/" + sessionBeanComponentDescription.getEJBName();
        String moduleJNDIBaseName = "java:module/" + sessionBeanComponentDescription.getEJBName();

        // the base ServiceName which will be used to create the ServiceName(s) for each of the view bindings
        ServiceName baseServiceName = deploymentUnit.getServiceName().append("component").append(sessionBeanComponentDescription.getComponentName());
        String ejbClassName = sessionBeanComponentDescription.getEJBClassName();
        // now create the bindings
        final BindingDescription globalBinding = new BindingDescription();
        globalBinding.setAbsoluteBinding(true);
        String globalJNDIName = globalJNDIBaseName + "!" + ejbClassName;
        globalBinding.setBindingName(globalJNDIName);
        globalBinding.setBindingType(ejbClassName);
        globalBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(ejbClassName)));
        // add the binding to the component description
        componentDescription.getBindings().add(globalBinding);
        logger.debug("Added java:global jndi binding at " + globalJNDIName + " for no-interface view of session bean: " + sessionBeanComponentDescription.getEJBName());

        // java:app bindings
        final BindingDescription appBinding = new BindingDescription();
        appBinding.setAbsoluteBinding(true);
        String appJNDIName = appJNDIBaseName + "!" + ejbClassName;
        appBinding.setBindingName(appJNDIName);
        appBinding.setBindingType(ejbClassName);
        appBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(ejbClassName)));
        // add the binding to the component description
        componentDescription.getBindings().add(appBinding);
        logger.debug("Added java:app jndi binding at " + appJNDIName + " for no-interface view of session bean: " + sessionBeanComponentDescription.getEJBName());

        // java:module bindings
        final BindingDescription moduleBinding = new BindingDescription();
        moduleBinding.setAbsoluteBinding(true);
        String moduleJNDIName = moduleJNDIBaseName + "!" + ejbClassName;
        moduleBinding.setBindingName(moduleJNDIName);
        moduleBinding.setBindingType(ejbClassName);
        moduleBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(ejbClassName)));
        // add the binding to the component description
        componentDescription.getBindings().add(moduleBinding);
        logger.debug("Added java:module jndi binding at " + moduleJNDIName + " for no-interface view of session bean: " + sessionBeanComponentDescription.getEJBName());

    }

    /**
     * Returns true if the <code>sessionBeanClass</code> has a {@link LocalBean no-interface view annotation}.
     * Else returns false.
     *
     * @param sessionBeanClass The session bean {@link ClassInfo class}
     * @return
     */
    private boolean hasNoInterfaceView(ClassInfo sessionBeanClass) {
        Map<DotName, List<AnnotationInstance>> annotationsOnBeanClass = sessionBeanClass.annotations();
        if (annotationsOnBeanClass == null || annotationsOnBeanClass.isEmpty()) {
            return false;
        }
        List<AnnotationInstance> localBeanAnnotations = annotationsOnBeanClass.get(DotName.createSimple(LocalBean.class.getName()));
        return localBeanAnnotations != null && !localBeanAnnotations.isEmpty();
    }

    /**
     * Returns the name (stripped off the .ear suffix) of the top level .ear deployment for the passed <code>deploymentUnit</code>.
     * Returns null if the passed <code>deploymentUnit</code> doesn't belong to a .ear deployment.
     *
     * @param deploymentUnit
     * @return TODO: Move this method to some common place
     */
    private String getEarName(DeploymentUnit deploymentUnit) {
        DeploymentUnit parentDU = deploymentUnit.getParent();
        if (parentDU == null) {
            String duName = deploymentUnit.getName();
            if (duName.endsWith(".ear")) {
                return duName.substring(0, duName.length() - ".ear".length());
            }
            return null;
        }
        // traverse to top level DU
        while (parentDU.getParent() != null) {
            parentDU = parentDU.getParent();
        }
        String duName = parentDU.getName();
        if (duName.endsWith(".ear")) {
            return duName.substring(0, duName.length() - ".ear".length());
        }
        return null;
    }
}
