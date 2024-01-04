/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * The {@link ResourceDefinitionAnnotationProcessor} for {@link ManagedExecutorDefinition}.
 * @author emmartins
 */
public class ManagedExecutorDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName MANAGED_EXECUTOR_DEFINITION = DotName.createSimple(ManagedExecutorDefinition.class.getName());
    private static final DotName MANAGED_EXECUTOR_DEFINITION_LIST = DotName.createSimple(ManagedExecutorDefinition.List.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return MANAGED_EXECUTOR_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return MANAGED_EXECUTOR_DEFINITION_LIST;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String jndiName = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final String context = AnnotationElement.asOptionalString(annotationInstance, ManagedExecutorDefinitionInjectionSource.CONTEXT_PROP);
        final String hungTaskThresholdString = AnnotationElement.asOptionalString(annotationInstance, ManagedExecutorDefinitionInjectionSource.HUNG_TASK_THRESHOLD_PROP);
        final long hungTaskThreshold = hungTaskThresholdString != null && !hungTaskThresholdString.isEmpty() ? Math.max(Long.valueOf(hungTaskThresholdString), 0L) : 0L;
        final int maxAsync = AnnotationElement.asOptionalInt(annotationInstance, ManagedExecutorDefinitionInjectionSource.MAX_ASYNC_PROP);
        final ManagedExecutorDefinitionInjectionSource injectionSource = new ManagedExecutorDefinitionInjectionSource(jndiName);
        injectionSource.setContextServiceRef(context);
        injectionSource.setHungTaskThreshold(hungTaskThreshold);
        injectionSource.setMaxAsync(maxAsync);
        return injectionSource;
    }
}
