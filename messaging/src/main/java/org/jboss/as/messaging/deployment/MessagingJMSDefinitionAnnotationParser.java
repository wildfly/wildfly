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
import static org.jboss.as.ee.structure.DeploymentType.APPLICATION_CLIENT;
import static org.jboss.as.ee.structure.DeploymentType.WAR;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
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
 * Process {@link JMSDestinationDefinition}(s) {@link JMSConnectionFactoryDefinition}(s) annotations.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class MessagingJMSDefinitionAnnotationParser implements DeploymentUnitProcessor {

    private static final DotName JMS_DESTINATION_DEFINITION = DotName.createSimple(JMSDestinationDefinition.class.getName());
    private static final DotName JMS_DESTINATION_DEFINITIONS = DotName.createSimple(JMSDestinationDefinitions.class.getName());
    private static final DotName JMS_CONNECTION_FACTORY_DEFINITION = DotName.createSimple(JMSConnectionFactoryDefinition.class.getName());
    private static final DotName JMS_CONNECTION_FACTORY_DEFINITIONS = DotName.createSimple(JMSConnectionFactoryDefinitions.class.getName());

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
                processJMSDestinationDefinition(deploymentUnit, eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
            }
        }

        // @JMSDestinationDefinition
        for (AnnotationInstance definition : index.getAnnotations(JMS_DESTINATION_DEFINITION)) {
            final AnnotationTarget target = definition.target();
            if (!(target instanceof ClassInfo)) {
                throw MESSAGES.classOnlyAnnotation(JMS_DESTINATION_DEFINITION.toString(), target);
            }
            processJMSDestinationDefinition(deploymentUnit, eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
        }

        // @JMSConnectionFactoryDefinitions
        for (AnnotationInstance annotation : index.getAnnotations(JMS_CONNECTION_FACTORY_DEFINITIONS)) {
            final AnnotationTarget target = annotation.target();
            if (!(target instanceof ClassInfo)) {
                throw MESSAGES.classOnlyAnnotation(JMS_CONNECTION_FACTORY_DEFINITIONS.toString(), target);
            }
            List<AnnotationInstance> connectionFactoryDefinitions = getNestedDefinitionAnnotations(annotation);
            for (AnnotationInstance definition : connectionFactoryDefinitions) {
                processJMSConnectionFactoryDefinition(deploymentUnit, eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
            }
        }

        // @JMSConnectionFactoryDefinition
        for (AnnotationInstance definition : index.getAnnotations(JMS_CONNECTION_FACTORY_DEFINITION)) {
            final AnnotationTarget target = definition.target();
            if (!(target instanceof ClassInfo)) {
                throw MESSAGES.classOnlyAnnotation(JMS_CONNECTION_FACTORY_DEFINITION.toString(), target);
            }
            processJMSConnectionFactoryDefinition(deploymentUnit, eeModuleDescription, definition, (ClassInfo) target, applicationClasses);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processJMSDestinationDefinition(DeploymentUnit deploymentUnit, EEModuleDescription eeModuleDescription, AnnotationInstance destinationDefinition, ClassInfo target, EEApplicationClasses applicationClasses) {
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

        if (DeploymentTypeMarker.isType(WAR, deploymentUnit) || DeploymentTypeMarker.isType(APPLICATION_CLIENT, deploymentUnit)) {
            eeModuleDescription.getBindingConfigurations().add(config);
        }

        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(target.name().toString());
        classDescription.getBindingConfigurations().add(config);
    }

    private void processJMSConnectionFactoryDefinition(DeploymentUnit deploymentUnit, EEModuleDescription eeModuleDescription, AnnotationInstance connectionFactoryDefinition, ClassInfo target, EEApplicationClasses applicationClasses) {
        final AnnotationValue nameValue = connectionFactoryDefinition.value(NAME);
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw MESSAGES.annotationAttributeMissing(JMS_CONNECTION_FACTORY_DEFINITION.toString(), NAME);
        }

        DirectJMSConnectionFactoryInjectionSource source = new DirectJMSConnectionFactoryInjectionSource(nameValue.asString());
        source.setInterfaceName(asString(connectionFactoryDefinition, "interfaceName", ConnectionFactory.class.getName()));
        source.setResourceAdapter(asString(connectionFactoryDefinition, "resourceAdapter"));
        source.setUser(asString(connectionFactoryDefinition, "user"));
        source.setPassword(asString(connectionFactoryDefinition, "password"));
        source.setClientId(asString(connectionFactoryDefinition, "clientId"));
        for (String fullProp : asArray(connectionFactoryDefinition, "properties")) {
            String[] prop = fullProp.split("=", 2);
            source.addProperty(prop[0], prop[1]);
        }
        source.setTransactional(asBoolean(connectionFactoryDefinition, "transactional"));
        source.setMaxPoolSize(asInt(connectionFactoryDefinition, "maxPoolSize", MAX_POOL_SIZE.getDefaultValue().asInt()));
        source.setMinPoolSize(asInt(connectionFactoryDefinition, "minPoolSize", MIN_POOL_SIZE.getDefaultValue().asInt()));

        final BindingConfiguration config = new BindingConfiguration(nameValue.asString(), source);

        if (DeploymentTypeMarker.isType(WAR, deploymentUnit)
                || DeploymentTypeMarker.isType(APPLICATION_CLIENT, deploymentUnit)) {
            eeModuleDescription.getBindingConfigurations().add(config);
        }

        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(target.name().toString());
        classDescription.getBindingConfigurations().add(config);
    }

    /**
     * Returns the nested {@link JMSDestinationDefinition} (resp. {@link JMSConnectionFactoryDefinition}) annotations out
     * of the outer {@link JMSDestinationDefinitions} (resp. {@link JMSConnectionFactoryDefinitions}) annotation
     *
     * @param definitions The outer {@link JMSDestinationDefinitions} (resp. {@link JMSConnectionFactoryDefinitions}) annotation
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

    private static boolean asBoolean(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? true : value.asBoolean();
    }

    private static int asInt(final AnnotationInstance annotation, String property, int defaultValue) {
        AnnotationValue value = annotation.value(property);
        return value == null ? defaultValue : value.asInt();
    }


    private static String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? new String[0] : value.asStringArray();
    }
}
