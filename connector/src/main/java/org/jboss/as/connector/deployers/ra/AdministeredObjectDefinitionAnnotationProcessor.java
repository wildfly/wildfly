/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.resource.AdministeredObjectDefinition;
import jakarta.resource.AdministeredObjectDefinitions;

/**
 * Deployment processor responsible for processing {@link jakarta.resource.AdministeredObjectDefinition} and {@link jakarta.resource.AdministeredObjectDefinitions}.
 *
 * @author Jesper Pedersen
 * @author Eduardo Martins
 */
public class AdministeredObjectDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName ANNOTATION_NAME = DotName.createSimple(AdministeredObjectDefinition.class.getName());
    private static final DotName COLLECTION_ANNOTATION_NAME = DotName.createSimple(AdministeredObjectDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return ANNOTATION_NAME;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return COLLECTION_ANNOTATION_NAME;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String name = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final String className = AnnotationElement.asRequiredString(annotationInstance, "className");
        final String ra = AnnotationElement.asRequiredString(annotationInstance, "resourceAdapter");
        final AdministeredObjectDefinitionInjectionSource directAdministeredObjectInjectionSource =
                new AdministeredObjectDefinitionInjectionSource(name, className, ra);
        directAdministeredObjectInjectionSource.setDescription(AnnotationElement.asOptionalString(annotationInstance,
                AdministeredObjectDefinitionInjectionSource.DESCRIPTION));
        directAdministeredObjectInjectionSource.setInterface(AnnotationElement.asOptionalString(annotationInstance,
                AdministeredObjectDefinitionInjectionSource.INTERFACE));
        directAdministeredObjectInjectionSource.addProperties(AnnotationElement.asOptionalStringArray(annotationInstance,
                AdministeredObjectDefinitionInjectionSource.PROPERTIES));
        return directAdministeredObjectInjectionSource;
    }

}
