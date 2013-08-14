/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.deployment;

import static org.jboss.as.ee.EeMessages.MESSAGES;
import static org.jboss.as.messaging.CommonAttributes.NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Process {@link JMSDestinationDefinition}(s) annotations.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class MessagingJMSDefinitionAnnotationParser implements DeploymentUnitProcessor {

    private static final DotName JMS_DESTINATION_DEFINITION = DotName.createSimple(JMSDestinationDefinition.class.getName());
    private static final DotName JMS_DESTINATION_DEFINITIONS = DotName.createSimple(JMSDestinationDefinitions.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        // @JMSDestinationDefinitions
        for (AnnotationInstance annotation : index.getAnnotations(JMS_DESTINATION_DEFINITIONS)) {
            final AnnotationTarget target = annotation.target();
            if (!(target instanceof ClassInfo)) {
                throw MESSAGES.classOnlyAnnotation(JMS_DESTINATION_DEFINITIONS.toString(), target);
            }
            List<AnnotationInstance> destinationDefinitions = getNestedDefinitionAnnotations(annotation);
            for (AnnotationInstance definition : destinationDefinitions) {
                processJMSDestinationDefinition(eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
            }
        }

        // @JMSDestinationDefinition
        for (AnnotationInstance definition : index.getAnnotations(JMS_DESTINATION_DEFINITION)) {
            final AnnotationTarget target = definition.target();
            if (!(target instanceof ClassInfo)) {
                throw MESSAGES.classOnlyAnnotation(JMS_DESTINATION_DEFINITION.toString(), target);
            }
            processJMSDestinationDefinition(eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processJMSDestinationDefinition(EEModuleDescription eeModuleDescription, AnnotationInstance destinationDefinition, ClassInfo target, EEApplicationClasses applicationClasses) {
        final AnnotationValue nameValue = destinationDefinition.value(NAME);
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw MESSAGES.annotationAttributeMissing(JMS_DESTINATION_DEFINITION.toString(), NAME);
        }

        final AnnotationValue interfaceNameValue = destinationDefinition.value("interfaceName");
        if (interfaceNameValue == null || interfaceNameValue.asString().isEmpty()) {
            throw MESSAGES.annotationAttributeMissing(JMS_DESTINATION_DEFINITION.toString(), "interfaceName");
        }

        DirectJMSDestinationInjectionSource source = new DirectJMSDestinationInjectionSource(nameValue.asString(), interfaceNameValue.asString());
        source.setDestinationName(asString(destinationDefinition, "destinationName"));
        for (String fullProp : asArray(destinationDefinition, "properties")) {
            String[] prop = fullProp.split("=", 2);
            source.addProperty(prop[0], prop[1]);
        }

        final BindingConfiguration config = new BindingConfiguration(nameValue.asString(), source);
        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(target.name().toString());
        classDescription.getBindingConfigurations().add(config);
    }

    /**
     * Returns the nested {@link JMSDestinationDefinition} annotations out
     * of the outer {@link JMSDestinationDefinitions} annotation
     *
     * @param definitions The outer {@link JMSDestinationDefinitions} annotation
     */
    private static List<AnnotationInstance> getNestedDefinitionAnnotations(AnnotationInstance definitions) {
        if (definitions == null) {
            return Collections.emptyList();
        }
        AnnotationInstance[] nested = definitions.value().asNestedArray();
        return Arrays.asList(nested);
    }

    private static String asString(final AnnotationInstance annotation, String property) {
        return asString(annotation, property, "");
    }

    private static String asString(final AnnotationInstance annotation, String property, String defaultValue) {
        AnnotationValue value = annotation.value(property);
        return value == null ? defaultValue : value.asString().isEmpty() ? defaultValue : value.asString();
    }

    private static String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? new String[0] : value.asStringArray();
    }
}
