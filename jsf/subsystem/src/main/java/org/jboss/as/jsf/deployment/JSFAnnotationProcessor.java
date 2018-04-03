/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsf.deployment;

import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;

import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link DeploymentUnitProcessor} implementation responsible for extracting JSF annotations from a deployment and attaching them
 * to the deployment unit to eventually be added to the {@link javax.servlet.ServletContext}.
 *
 * @author John Bailey
 */
public class JSFAnnotationProcessor implements DeploymentUnitProcessor {

    public static final String FACES_ANNOTATIONS_SC_ATTR =  "org.jboss.as.jsf.FACES_ANNOTATIONS";

    private enum FacesAnnotation {
        FACES_COMPONENT(FacesComponent.class),
        FACES_CONVERTER(FacesConverter.class),
        FACES_VALIDATOR(FacesValidator.class),
        FACES_RENDERER(FacesRenderer.class),
        MANAGED_BEAN(ManagedBean.class),
        NAMED_EVENT(NamedEvent.class),
        FACES_BEHAVIOR(FacesBehavior.class),
        FACES_BEHAVIOR_RENDERER(FacesBehaviorRenderer.class);

        private final Class<? extends Annotation> annotationClass;
        private final DotName indexName;

        private FacesAnnotation(Class<? extends Annotation> annotationClass) {
            this.annotationClass = annotationClass;
            this.indexName = DotName.createSimple(annotationClass.getName());
        }
    }


    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }

        final Map<Class<? extends Annotation>, Set<Class<?>>> instances = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return; // Can not continue without index
        }

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            return; // Can not continue without module
        }
        final ClassLoader classLoader = module.getClassLoader();

        for (FacesAnnotation annotation : FacesAnnotation.values()) {
            final List<AnnotationInstance> annotationInstances = compositeIndex.getAnnotations(annotation.indexName);
            if (annotationInstances == null || annotationInstances.isEmpty()) {
                continue;
            }
            final Set<Class<?>> discoveredClasses = new HashSet<Class<?>>();
            instances.put(annotation.annotationClass, discoveredClasses);
            for (AnnotationInstance annotationInstance : annotationInstances) {
                final AnnotationTarget target = annotationInstance.target();
                if (target instanceof ClassInfo) {
                    final DotName className = ClassInfo.class.cast(target).name();
                    final Class<?> annotatedClass;
                    try {
                        annotatedClass = classLoader.loadClass(className.toString());
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException(JSFLogger.ROOT_LOGGER.classLoadingFailed(className));
                    }
                    discoveredClasses.add(annotatedClass);
                } else {
                    throw new DeploymentUnitProcessingException(JSFLogger.ROOT_LOGGER.invalidAnnotationLocation(annotation, target));
                }
            }
        }
        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(FACES_ANNOTATIONS_SC_ATTR, instances));
    }

    public void undeploy(DeploymentUnit context) {

    }
}
