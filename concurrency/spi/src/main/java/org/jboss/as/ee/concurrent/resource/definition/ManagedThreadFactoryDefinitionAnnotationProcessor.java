/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * The {@link ResourceDefinitionAnnotationProcessor} for {@link ManagedThreadFactoryDefinition}.
 * @author emmartins
 */
public class ManagedThreadFactoryDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName MANAGED_THREAD_FACTORY_DEFINITION = DotName.createSimple(ManagedThreadFactoryDefinition.class.getName());
    private static final DotName MANAGED_THREAD_FACTORY_DEFINITION_LIST = DotName.createSimple(ManagedThreadFactoryDefinition.List.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return MANAGED_THREAD_FACTORY_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return MANAGED_THREAD_FACTORY_DEFINITION_LIST;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String jndiName = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final String context = AnnotationElement.asOptionalString(annotationInstance, ManagedThreadFactoryDefinitionInjectionSource.CONTEXT_PROP);
        final int priority = AnnotationElement.asOptionalInt(annotationInstance, ManagedThreadFactoryDefinitionInjectionSource.PRIORITY_PROP);
        final ManagedThreadFactoryDefinitionInjectionSource injectionSource = new ManagedThreadFactoryDefinitionInjectionSource(jndiName);
        injectionSource.setContextServiceRef(context);
        injectionSource.setPriority(priority > 0 ? priority : Thread.NORM_PRIORITY);
        return injectionSource;
    }
}
