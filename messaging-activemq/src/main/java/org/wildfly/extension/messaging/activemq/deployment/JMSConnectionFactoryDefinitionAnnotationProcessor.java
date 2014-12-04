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

package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE;

import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * {@link javax.jms.JMSConnectionFactoryDefinition}(s) resource config annotation processor.
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
        directJMSConnectionFactoryInjectionSource.setInterfaceName(AnnotationElement.asOptionalString(annotationInstance, "interfaceName", "javax.jms.ConnectionFactory", propertyReplacer));
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
