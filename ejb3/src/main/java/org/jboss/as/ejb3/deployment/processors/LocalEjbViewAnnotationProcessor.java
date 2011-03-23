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
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import javax.ejb.Local;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Processes {@link Local @Local} annotation of a session bean and sets up the {@link SessionBeanComponentDescription}
 * out of it.
 * <p/>
 *
 * @author Jaikiran Pai
 */
public class LocalEjbViewAnnotationProcessor extends AbstractComponentConfigProcessor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(LocalEjbViewAnnotationProcessor.class);

    /**
     * Creates bindings for each of the local business interface view of a session bean.
     * Also, adds the local business interface views to the component description.
     *
     * @param deploymentUnit       The deployment unit
     * @param phaseContext         The phase context
     * @param compositeIndex       The composite annotation index
     * @param componentDescription The component description
     * @throws DeploymentUnitProcessingException
     *
     */
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
        // fetch the local business interfaces of the bean
        Collection<String> localBusinessInterfaces = this.getLocalBusinessInterfaces(sessionBeanComponentDescription, compositeIndex, sessionBeanClass);
        if (logger.isTraceEnabled()) {
            logger.trace("Session bean: " + sessionBeanComponentDescription.getEJBName() + " has " + localBusinessInterfaces.size() + " local business interfaces namely: " + localBusinessInterfaces);
        }
        // add it to the component description
        sessionBeanComponentDescription.addLocalBusinessInterfaceViews(localBusinessInterfaces);

    }

    // TODO: This isn't yet completely implemented. This currently only takes into account @Local annotation on the
    // bean implementation class. This further has to check for @Local on the interfaces implemented by the bean
    private Collection<String> getLocalBusinessInterfaces(SessionBeanComponentDescription description, CompositeIndex compositeIndex, ClassInfo sessionBeanClass) throws DeploymentUnitProcessingException {
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = sessionBeanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            String defaultLocalBusinessInterface = this.getDefaultLocalInterface(sessionBeanClass);
            if (defaultLocalBusinessInterface != null) {
                logger.debug("Session bean class: " + sessionBeanClass + " has no explicit local business interfaces, marking " + defaultLocalBusinessInterface + " as the (implicit) default local business interface");
                return Collections.singleton(defaultLocalBusinessInterface);
            }

            return Collections.emptySet();
        }
        List<AnnotationInstance> ejbLocalAnnotations = annotationsOnBean.get(DotName.createSimple(Local.class.getName()));
        if (ejbLocalAnnotations == null || ejbLocalAnnotations.isEmpty()) {
            String defaultLocalBusinessInterface = this.getDefaultLocalInterface(sessionBeanClass);
            if (defaultLocalBusinessInterface != null) {
                logger.debug("Session bean class: " + sessionBeanClass + " has no explicit local business interfaces, marking " + defaultLocalBusinessInterface + " as the (implicit) default local business interface");
                return Collections.singleton(defaultLocalBusinessInterface);
            }

            return Collections.emptySet();
        }
        if (ejbLocalAnnotations.size() > 1) {
            throw new RuntimeException("@Local appears more than once in EJB class: " + sessionBeanClass.name());
        }
        final Collection<String> localBusinessInterfaces = new HashSet<String>();

        AnnotationInstance ejbLocalAnnotation = ejbLocalAnnotations.get(0);
        AnnotationTarget target = ejbLocalAnnotation.target();
        if (target instanceof ClassInfo == false) {
            throw new RuntimeException("@Local should only appear on a class. Target: " + target + " is not a class");
        }
        AnnotationValue ejbLocalAnnValue = ejbLocalAnnotation.value();
        if (ejbLocalAnnValue == null) {
            DotName[] interfaces = sessionBeanClass.interfaces();
            if (interfaces.length != 1)
                throw new DeploymentUnitProcessingException("Bean " + description + " specifies @Local, but does not implement 1 interface");
            localBusinessInterfaces.add(interfaces[0].toString());
            return localBusinessInterfaces;
        }
        Type[] localInterfaceTypes = ejbLocalAnnValue.asClassArray();
        for (Type localInterface : localInterfaceTypes) {
            localBusinessInterfaces.add(localInterface.name().toString());
        }
        return localBusinessInterfaces;
    }

    private String getDefaultLocalInterface(ClassInfo sessionBeanClass) {
        DotName[] interfaces = sessionBeanClass.interfaces();
        // TODO: Needs better implementation. For now, we are just checking for a single interface on the
        // bean implementation class. The EJB spec mandates skipping some specific interfaces like java.io.Serializable
        // and such. All such rules need to be applied here.
        if (interfaces == null || interfaces.length != 1) {
            return null;
        }
        return interfaces[0].toString();
    }

}
