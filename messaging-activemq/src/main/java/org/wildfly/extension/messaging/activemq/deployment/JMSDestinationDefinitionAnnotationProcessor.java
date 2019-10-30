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

import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

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
