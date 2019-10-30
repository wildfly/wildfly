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

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;

/**
 * Deployment processor responsible for processing {@link javax.resource.AdministeredObjectDefinition} and {@link javax.resource.AdministeredObjectDefinitions}.
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
