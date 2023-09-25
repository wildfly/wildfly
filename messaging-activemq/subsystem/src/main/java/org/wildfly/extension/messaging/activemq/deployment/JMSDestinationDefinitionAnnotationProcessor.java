/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * {@link JMSDestinationDefinition}(s) resource config annotation processor.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 * @author Eduardo Martins
 */
public class JMSDestinationDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName JMS_DESTINATION_DEFINITION = DotName.createSimple(JMSDestinationDefinition.class.getName());
    private static final DotName JMS_DESTINATION_DEFINITIONS = DotName.createSimple(JMSDestinationDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return JMS_DESTINATION_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return JMS_DESTINATION_DEFINITIONS;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final String jndiName = AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME);
        final String interfaceName = AnnotationElement.asRequiredString(annotationInstance, "interfaceName");
        final JMSDestinationDefinitionInjectionSource directJMSDestinationInjectionSource = new JMSDestinationDefinitionInjectionSource(jndiName, interfaceName);
        directJMSDestinationInjectionSource.setClassName(AnnotationElement.asOptionalString(annotationInstance, "className"));
        directJMSDestinationInjectionSource.setResourceAdapter(AnnotationElement.asOptionalString(annotationInstance, "resourceAdapter"));
        directJMSDestinationInjectionSource.setDestinationName(AnnotationElement.asOptionalString(annotationInstance, "destinationName", propertyReplacer));
        directJMSDestinationInjectionSource.addProperties(AnnotationElement.asOptionalStringArray(annotationInstance, AnnotationElement.PROPERTIES), propertyReplacer);
        return directJMSDestinationInjectionSource;
    }

}
