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
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;
import javax.resource.spi.TransactionSupport;

/**
 * Deployment processor responsible for processing {@link javax.resource.ConnectionFactoryDefinition} and {@link javax.resource.ConnectionFactoryDefinitions}.
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
