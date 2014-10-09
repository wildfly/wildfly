/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.resource.definition;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
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
import org.jboss.metadata.property.PropertyReplacer;

import java.util.List;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * The foundation to create processors wrt deployment of classes annotated with EE Resource Definitions, as defined by EE.5.18.
 *
 * @author Eduardo Martins
 */
public abstract class ResourceDefinitionAnnotationProcessor implements DeploymentUnitProcessor {

    /**
     * Retrieves the annotation's dot name.
     * @return
     */
    protected abstract DotName getAnnotationDotName();

    /**
     * Retrieves the annotation collection's dot name.
     * @return
     */
    protected abstract DotName getAnnotationCollectionDotName();

    /**
     * Processes an annotation instance.
     * @param annotationInstance the annotation instance
     * @param propertyReplacer the property replacer which the processor may use to resolve annotation element values
     * @return a resource definition injection source
     * @throws DeploymentUnitProcessingException
     */
    protected abstract ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }
        final PropertyReplacer propertyReplacer = EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit);
        final DotName annotationName = getAnnotationDotName();
        for (AnnotationInstance annotationInstance : index.getAnnotations(annotationName)) {
            final List<BindingConfiguration> bindingConfigurations = getAnnotatedClassBindingConfigurations(moduleDescription, annotationInstance);
            final ResourceDefinitionInjectionSource injectionSource = processAnnotation(annotationInstance, propertyReplacer);
            bindingConfigurations.add(new BindingConfiguration(injectionSource.getJndiName(),injectionSource));
        }
        final DotName collectionAnnotationName = getAnnotationCollectionDotName();
        if (collectionAnnotationName != null) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(collectionAnnotationName)) {
                final AnnotationInstance[] nestedAnnotationInstances = annotationInstance.value().asNestedArray();
                if (nestedAnnotationInstances != null && nestedAnnotationInstances.length > 0) {
                    final List<BindingConfiguration> bindingConfigurations = getAnnotatedClassBindingConfigurations(moduleDescription, annotationInstance);
                    for (AnnotationInstance nestedAnnotationInstance : nestedAnnotationInstances) {
                        final ResourceDefinitionInjectionSource injectionSource = processAnnotation(nestedAnnotationInstance, propertyReplacer);
                        bindingConfigurations.add(new BindingConfiguration(injectionSource.getJndiName(),injectionSource));
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private List<BindingConfiguration> getAnnotatedClassBindingConfigurations(EEModuleDescription moduleDescription, AnnotationInstance annotationInstance) throws DeploymentUnitProcessingException {
        final AnnotationTarget target = annotationInstance.target();
        if (!(target instanceof ClassInfo)) {
            throw ROOT_LOGGER.classOnlyAnnotation(annotationInstance.toString(), target);
        }
        final ClassInfo classInfo = (ClassInfo) target;
        return moduleDescription.addOrGetLocalClassDescription(classInfo.name().toString()).getBindingConfigurations();
    }

    /**
     * Utility class to help handle resource definition annotation elements
     */
    public static class AnnotationElement {

        public static final String NAME = "name";
        public static final String PROPERTIES = "properties";

        public static boolean asOptionalBoolean(final AnnotationInstance annotation, String property) {
            AnnotationValue value = annotation.value(property);
            return value == null ? true : value.asBoolean();
        }

        public static int asOptionalInt(AnnotationInstance annotation, String string) {
            AnnotationValue value = annotation.value(string);
            return value == null ? -1 : value.asInt();
        }

        public static int asOptionalInt(AnnotationInstance annotation, String property, int defaultValue) {
            AnnotationValue value = annotation.value(property);
            return value == null ? defaultValue : value.asInt();
        }

        public static String asOptionalString(AnnotationInstance annotation, String property) {
            return asOptionalString(annotation, property, "", null);
        }

        public static String asOptionalString(AnnotationInstance annotation, String property, String defaultValue) {
            return asOptionalString(annotation, property, defaultValue, null);
        }

        public static String asOptionalString(AnnotationInstance annotation, String property, PropertyReplacer propertyReplacer) {
            return asOptionalString(annotation, property, "", propertyReplacer);
        }

        public static String asOptionalString(AnnotationInstance annotation, String property, String defaultValue, PropertyReplacer propertyReplacer) {
            AnnotationValue value = annotation.value(property);
            if (value == null) {
                return defaultValue;
            } else {
                String valueString = value.asString();
                if (valueString.isEmpty()) {
                    return defaultValue;
                } else {
                    return propertyReplacer != null ? propertyReplacer.replaceProperties(valueString) : valueString;
                }
            }
        }

        public static String[] asOptionalStringArray(AnnotationInstance annotation, String property) {
            AnnotationValue value = annotation.value(property);
            return value == null ? new String[0] : value.asStringArray();
        }

        public static String asRequiredString(AnnotationInstance annotationInstance, String attributeName) {
            return asRequiredString(annotationInstance, attributeName, null);
        }

        public static String asRequiredString(AnnotationInstance annotationInstance, final String attributeName, PropertyReplacer propertyReplacer) {
            final AnnotationValue nameValue = annotationInstance.value(attributeName);
            if (nameValue == null) {
                throw ROOT_LOGGER.annotationAttributeMissing(annotationInstance.name().toString(), attributeName);
            }
            final String nameValueAsString = nameValue.asString();
            if (nameValueAsString.isEmpty()) {
                throw ROOT_LOGGER.annotationAttributeMissing(annotationInstance.name().toString(), attributeName);
            }
            return propertyReplacer != null ? propertyReplacer.replaceProperties(nameValueAsString) : nameValueAsString;
        }
    }

}
