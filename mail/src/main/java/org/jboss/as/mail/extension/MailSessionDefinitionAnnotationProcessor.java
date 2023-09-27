/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.mail.MailSessionDefinition;
import jakarta.mail.MailSessionDefinitions;

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
