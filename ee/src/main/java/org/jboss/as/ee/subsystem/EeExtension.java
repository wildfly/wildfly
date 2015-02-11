/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.subsystem;

import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.ANNOTATIONS;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.META_INF;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SERVICES;

import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.ee.component.deployers.DefaultBindingsConfigurationProcessor;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * JBossAS domain extension used to initialize the ee subsystem handlers and associated classes.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "ee";
    private static final String RESOURCE_NAME = EeExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(3, 0, 0);

    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EeExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(EeSubsystemRootResource.create());

        // Mandatory describe operation
        rootResource.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // register submodels
        rootResource.registerSubModel(ContextServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedThreadFactoryResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedExecutorServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(ManagedScheduledExecutorServiceResourceDefinition.INSTANCE);
        rootResource.registerSubModel(new DefaultBindingsResourceDefinition(new DefaultBindingsConfigurationProcessor()));

        subsystem.registerXMLElementWriter(EESubsystemXmlPersister.INSTANCE);

        if (context.isRegisterTransformers()) {
            registerTransformers(subsystem);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_0.getUriString(), EESubsystemParser10.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_1.getUriString(), EESubsystemParser11.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_2_0.getUriString(), EESubsystemParser20.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_3_0.getUriString(), EESubsystemParser20.INSTANCE);
        context.setProfileParsingCompletionHandler(new BeanValidationProfileParsingCompletionHandler());
    }

    private void registerTransformers(SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        GlobalModulesRejecterConverter globalModulesRejecterConverter = new GlobalModulesRejecterConverter();

        builder.getAttributeBuilder()
                // Deal with https://issues.jboss.org/browse/AS7-4892 on 7.1.2
                .addRejectCheck(new JBossDescriptorPropertyReplacementRejectChecker(),
                        EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT)
                // Deal with new attributes added to global-modules elements
                .addRejectCheck(globalModulesRejecterConverter, GlobalModulesDefinition.INSTANCE)
                .setValueConverter(globalModulesRejecterConverter, GlobalModulesDefinition.INSTANCE)
                // Deal with new attribute annotation-property-replacement
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT);
        builder.rejectChildResource(PathElement.pathElement(EESubsystemModel.CONTEXT_SERVICE));
        builder.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY));
        builder.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE));
        builder.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        builder.discardChildResource(EESubsystemModel.DEFAULT_BINDINGS_PATH);

        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 0, 0));
    }

    /**
     * Due to https://issues.jboss.org/browse/AS7-4892 the jboss-descriptor-property-replacement attribute
     * does not get set properly in the model on 7.1.2; it remains undefined and defaults to 'true'.
     * So although the model version has not changed we register a transformer and reject it for 7.1.2 if it is set
     * and has a different value from 'true'
     */
    private static class JBossDescriptorPropertyReplacementRejectChecker extends RejectAttributeChecker.DefaultRejectAttributeChecker {

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {

            return EeLogger.ROOT_LOGGER.onlyTrueAllowedForJBossDescriptorPropertyReplacement_AS7_4892();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                ModelVersion version = context.getTarget().getVersion();
                if (version.getMajor() == 1 && version.getMinor() == 2) {
                    //7.1.2 has model version 1.2.0 and should have this transformation
                    //7.1.3 has model version 1.3.0 and should not have this transformation
                    if (attributeValue.getType() == ModelType.BOOLEAN) {
                        return !attributeValue.asBoolean();
                    } else {
                        if (!Boolean.parseBoolean(attributeValue.asString())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * Reject global-modules values with new fields if they differ from the legacy default. If not rejected
     * the converter removes the new fields.
     */
    private static class GlobalModulesRejecterConverter extends RejectAttributeChecker.DefaultRejectAttributeChecker implements AttributeConverter {

        private final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");

        @Override
        public void convertOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            cleanModel(attributeValue);
        }

        @Override
        public void convertResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            cleanModel(attributeValue);
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode node : attributeValue.asList()) {
                    if (node.hasDefined(ANNOTATIONS)) {
                        ModelNode annotations = node.get(ANNOTATIONS);
                        if (EXPRESSION_PATTERN.matcher(annotations.asString()).matches() || annotations.asBoolean()) {
                            return true;
                        }
                    }
                    if (node.hasDefined(SERVICES)) {
                        ModelNode services = node.get(SERVICES);
                        if (EXPRESSION_PATTERN.matcher(services.asString()).matches() || !services.asBoolean()) {
                            return true;
                        }
                    }
                    if (node.hasDefined(META_INF)) {
                        ModelNode metaInf = node.get(META_INF);
                        if (EXPRESSION_PATTERN.matcher(metaInf.asString()).matches() || metaInf.asBoolean()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return EeLogger.ROOT_LOGGER.propertiesNotAllowedOnGlobalModules();
        }

        private void cleanModel(final ModelNode attributeValue) {
            if (attributeValue.isDefined()) {
                for (ModelNode node : attributeValue.asList()) {
                    node.remove(ANNOTATIONS);
                    node.remove(SERVICES);
                    node.remove(META_INF);
                }
            }
        }
    }
}
