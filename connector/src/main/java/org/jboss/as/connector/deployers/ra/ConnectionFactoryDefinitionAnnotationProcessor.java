/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import jakarta.resource.ConnectionFactoryDefinition;
import jakarta.resource.ConnectionFactoryDefinitions;
import jakarta.resource.spi.TransactionSupport;

/**
 * Deployment processor responsible for processing {@link jakarta.resource.ConnectionFactoryDefinition} and {@link jakarta.resource.ConnectionFactoryDefinitions}.
 *
 * @author Jesper Pedersen
 * @author Eduardo Martins
 */
public class ConnectionFactoryDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName CONNECTION_FACTORY_DEFINITION = DotName.createSimple(ConnectionFactoryDefinition.class.getName());
    private static final DotName CONNECTION_FACTORY_DEFINITIONS = DotName.createSimple(ConnectionFactoryDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return CONNECTION_FACTORY_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return CONNECTION_FACTORY_DEFINITIONS;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String name = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final String interfaceName = AnnotationElement.asRequiredString(annotationInstance, "interfaceName");
        final String ra = AnnotationElement.asRequiredString(annotationInstance, "resourceAdapter");
        final ConnectionFactoryDefinitionInjectionSource directConnectionFactoryInjectionSource =
                new ConnectionFactoryDefinitionInjectionSource(name, interfaceName, ra);
        directConnectionFactoryInjectionSource.setDescription(AnnotationElement.asOptionalString(annotationInstance,
                ConnectionFactoryDefinitionInjectionSource.DESCRIPTION));
        directConnectionFactoryInjectionSource.setMaxPoolSize(AnnotationElement.asOptionalInt(annotationInstance,
                ConnectionFactoryDefinitionInjectionSource.MAX_POOL_SIZE));
        directConnectionFactoryInjectionSource.setMinPoolSize(AnnotationElement.asOptionalInt(annotationInstance,
                ConnectionFactoryDefinitionInjectionSource.MIN_POOL_SIZE));
        directConnectionFactoryInjectionSource.addProperties(AnnotationElement.asOptionalStringArray(annotationInstance,
                AnnotationElement.PROPERTIES));
        directConnectionFactoryInjectionSource.setTransactionSupportLevel(asTransactionSupportLocal(annotationInstance,
                ConnectionFactoryDefinitionInjectionSource.TRANSACTION_SUPPORT));
        return directConnectionFactoryInjectionSource;
    }

    private TransactionSupport.TransactionSupportLevel asTransactionSupportLocal(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : TransactionSupport.TransactionSupportLevel.valueOf((String)value.value());
    }

}
