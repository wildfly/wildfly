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

package org.jboss.as.mail.extension;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.MailSessionMetaData;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;
import org.jboss.metadata.property.PropertyReplacer;

import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;

/**
 * Deployment processor responsible for processing {@link MailSessionDefinition} and {@link MailSessionDefinitions}.
 *
 * @author Tomaz Cerar
 * @author Eduardo Martins
 */
public class MailSessionDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final DotName MAIL_SESSION_DEFINITION = DotName.createSimple(MailSessionDefinition.class.getName());
    private static final DotName MAIL_SESSION_DEFINITIONS = DotName.createSimple(MailSessionDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return MAIL_SESSION_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return MAIL_SESSION_DEFINITIONS;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final MailSessionMetaData metaData = new MailSessionMetaData();
        metaData.setName(AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME));
        metaData.setTransportProtocol(AnnotationElement.asOptionalString(annotationInstance, "transportProtocol"));
        metaData.setStoreProtocol(AnnotationElement.asOptionalString(annotationInstance, "storeProtocol"));
        metaData.setHost(AnnotationElement.asOptionalString(annotationInstance, "host"));
        metaData.setUser(AnnotationElement.asOptionalString(annotationInstance, "user"));
        metaData.setPassword(AnnotationElement.asOptionalString(annotationInstance, "password"));
        metaData.setFrom(AnnotationElement.asOptionalString(annotationInstance, "from"));
        final PropertiesMetaData properties = new PropertiesMetaData();
        for (String fullProp : AnnotationElement.asOptionalStringArray(annotationInstance, AnnotationElement.PROPERTIES)) {
            PropertyMetaData p = new PropertyMetaData();
            String[] prop = fullProp.split("=", 2);
            p.setName(prop[0]);
            p.setValue(prop[1]);
            properties.add(p);
        }
        metaData.setProperties(properties);
        final SessionProvider provider = SessionProviderFactory.create(metaData);
        return new MailSessionDefinitionInjectionSource(metaData.getName(), provider);
    }

}
