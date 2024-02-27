/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE;

import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSConnectionFactoryDefinitions;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * {@link jakarta.jms.JMSConnectionFactoryDefinition}(s) resource config annotation processor.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 * @author Eduardo Martins
 */
public class JMSConnectionFactoryDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName JMS_CONNECTION_FACTORY_DEFINITION = DotName.createSimple(JMSConnectionFactoryDefinition.class.getName());
    private static final DotName JMS_CONNECTION_FACTORY_DEFINITIONS = DotName.createSimple(JMSConnectionFactoryDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return JMS_CONNECTION_FACTORY_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return JMS_CONNECTION_FACTORY_DEFINITIONS;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final JMSConnectionFactoryDefinitionInjectionSource directJMSConnectionFactoryInjectionSource = new JMSConnectionFactoryDefinitionInjectionSource(AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME));
        directJMSConnectionFactoryInjectionSource.setResourceAdapter(AnnotationElement.asOptionalString(annotationInstance, "resourceAdapter"));
        directJMSConnectionFactoryInjectionSource.setInterfaceName(AnnotationElement.asOptionalString(annotationInstance, "interfaceName", "jakarta.jms.ConnectionFactory", propertyReplacer));
        directJMSConnectionFactoryInjectionSource.setUser(AnnotationElement.asOptionalString(annotationInstance, "user", propertyReplacer));
        directJMSConnectionFactoryInjectionSource.setPassword(AnnotationElement.asOptionalString(annotationInstance, "password", propertyReplacer));
        directJMSConnectionFactoryInjectionSource.setClientId(AnnotationElement.asOptionalString(annotationInstance, "clientId", propertyReplacer));
        directJMSConnectionFactoryInjectionSource.setTransactional(AnnotationElement.asOptionalBoolean(annotationInstance, "transactional"));
        directJMSConnectionFactoryInjectionSource.setMaxPoolSize(AnnotationElement.asOptionalInt(annotationInstance, "maxPoolSize", MAX_POOL_SIZE.getDefaultValue().asInt()));
        directJMSConnectionFactoryInjectionSource.setMinPoolSize(AnnotationElement.asOptionalInt(annotationInstance, "minPoolSize", MIN_POOL_SIZE.getDefaultValue().asInt()));
        directJMSConnectionFactoryInjectionSource.addProperties(AnnotationElement.asOptionalStringArray(annotationInstance, AnnotationElement.PROPERTIES), propertyReplacer);
        return directJMSConnectionFactoryInjectionSource;
    }

}
