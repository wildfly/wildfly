/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * The {@link ResourceDefinitionAnnotationProcessor} for {@link ContextServiceDefinition}.
 * @author emmartins
 */
public class ContextServiceDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    public static final String CLEARED_PROP = "cleared";
    public static final String PROPAGATED_PROP = "propagated";
    public static final String UNCHANGED_PROP = "unchanged";

    private static final DotName CONTEXT_SERVICE_DEFINITION = DotName.createSimple(ContextServiceDefinition.class.getName());
    private static final DotName CONTEXT_SERVICE_DEFINITION_LIST = DotName.createSimple(ContextServiceDefinition.List.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return CONTEXT_SERVICE_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return CONTEXT_SERVICE_DEFINITION_LIST;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String jndiName = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final ContextServiceTypesConfiguration contextServiceTypesConfiguration = new ContextServiceTypesConfiguration.Builder()
                .setCleared(AnnotationElement.asOptionalStringArray(annotationInstance, CLEARED_PROP))
                .setPropagated(AnnotationElement.asOptionalStringArray(annotationInstance, PROPAGATED_PROP))
                .setUnchanged(AnnotationElement.asOptionalStringArray(annotationInstance, UNCHANGED_PROP))
                .build();
        return new ContextServiceDefinitionInjectionSource(jndiName, contextServiceTypesConfiguration);
    }
}
