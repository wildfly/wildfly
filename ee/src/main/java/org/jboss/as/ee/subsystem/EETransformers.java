/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class EETransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return EeExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        final ModelVersion v1_0_0 = ModelVersion.create(1, 0, 0); //EAP 6.2.0
        final ModelVersion v1_1_0 = ModelVersion.create(1, 1, 0);
        final ModelVersion v3_0_0 = ModelVersion.create(3, 0, 0);
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());
        ResourceTransformationDescriptionBuilder builder_3_0 = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), v3_0_0);

        ManagedExecutorServiceResourceDefinition.INSTANCE.registerTransformers_4_0(builder_3_0);
        ManagedScheduledExecutorServiceResourceDefinition.INSTANCE.registerTransformers_4_0(builder_3_0);


        // 3.0.0 --> 1.1.0
        ResourceTransformationDescriptionBuilder builder11 = chainedBuilder.createBuilder(v3_0_0, v1_1_0);
        builder11.rejectChildResource(PathElement.pathElement(EESubsystemModel.CONTEXT_SERVICE));
        builder11.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY));
        builder11.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE));
        builder11.rejectChildResource(PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE));
        builder11.discardChildResource(EESubsystemModel.DEFAULT_BINDINGS_PATH);

        // 1.1.0 --> 1.0.0
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(v1_1_0, v1_0_0);
        GlobalModulesRejecterConverter globalModulesRejecterConverter = new GlobalModulesRejecterConverter();
        builder.getAttributeBuilder()
                // Deal with new attributes added to global-modules elements
                .addRejectCheck(globalModulesRejecterConverter, GlobalModulesDefinition.INSTANCE)
                .setValueConverter(globalModulesRejecterConverter, GlobalModulesDefinition.INSTANCE)
                // Deal with new attribute annotation-property-replacement
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT);


        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{
                v1_0_0,
                v1_1_0,
                v3_0_0
        });
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
