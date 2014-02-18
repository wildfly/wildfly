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

package org.jboss.as.modcluster;

import static org.jboss.as.modcluster.CustomLoadMetricDefinition.CLASS;
import static org.jboss.as.modcluster.DynamicLoadProviderDefinition.DECAY;
import static org.jboss.as.modcluster.DynamicLoadProviderDefinition.HISTORY;
import static org.jboss.as.modcluster.LoadMetricDefinition.CAPACITY;
import static org.jboss.as.modcluster.LoadMetricDefinition.PROPERTY;
import static org.jboss.as.modcluster.LoadMetricDefinition.TYPE;
import static org.jboss.as.modcluster.LoadMetricDefinition.WEIGHT;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.ADVERTISE;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.AUTO_ENABLE_CONTEXTS;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.FLUSH_PACKETS;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.PING;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.SESSION_DRAINING_STRATEGY;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.PROTOCOL;

import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DefaultCheckersAndConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 * @author Radoslav Husar
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    public static final String SUBSYSTEM_NAME = "modcluster";
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 5;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);
    static final PathElement CONFIGURATION_PATH = PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION);
    static final PathElement SSL_CONFIGURATION_PATH = PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION);
    static final PathElement DYNAMIC_LOAD_PROVIDER_PATH = PathElement.pathElement(CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.CONFIGURATION);
    static final PathElement LOAD_METRIC_PATH = PathElement.pathElement(CommonAttributes.LOAD_METRIC);
    static final PathElement CUSTOM_LOAD_METRIC_PATH = PathElement.pathElement(CommonAttributes.CUSTOM_LOAD_METRIC);

    private static final String RESOURCE_NAME = ModClusterExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ModClusterExtension.class.getClassLoader(), true, false);
    }

    static final SensitivityClassification MOD_CLUSTER_SECURITY =
          new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-security", false, true, true);

    static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(MOD_CLUSTER_SECURITY);

    static final SensitivityClassification MOD_CLUSTER_PROXIES =
            new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-proxies", false, false, false);

    static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_PROXIES_DEF = new SensitiveTargetAccessConstraintDefinition(MOD_CLUSTER_PROXIES);

    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debugf("Activating mod_cluster extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ModClusterDefinition(context.isRuntimeOnlyRegistrationValid()));

        final ManagementResourceRegistration configuration = registration.registerSubModel(new ModClusterConfigResourceDefinition());

        configuration.registerSubModel(new ModClusterSSLResourceDefinition());
        final ManagementResourceRegistration dynamicLoadProvider = configuration.registerSubModel(DynamicLoadProviderDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(LoadMetricDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(CustomLoadMetricDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(new ModClusterSubsystemXMLWriter());

        if (context.isRegisterTransformers()) {
            registerTransformers_1_2_0(subsystem);
            registerTransformers_1_3_0(subsystem); // AS 7.2.0
            registerTransformers_1_4_0(subsystem); // EAP 6.2.0
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), namespace.getXMLReader());
            }
        }
    }

    /**
     * Transformation that rejects expressions, discards/rejects session draining strategy, and converts load provider capacity.
     *
     * @param subsystem
     */
    private static void registerTransformers_1_2_0(SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder configurationBuilder = builder.addChildResource(CONFIGURATION_PATH);
        ResourceTransformationDescriptionBuilder dynamicLoadProvider = configurationBuilder
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ADVERTISE, AUTO_ENABLE_CONTEXTS, FLUSH_PACKETS, STICKY_SESSION, STICKY_SESSION_REMOVE, STICKY_SESSION_FORCE, PING)
                .addRejectCheck(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                .setDiscard(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                .end()
            .addChildResource(DYNAMIC_LOAD_PROVIDER_PATH)
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, DECAY, HISTORY)
                    .end();
        dynamicLoadProvider.addChildResource(CUSTOM_LOAD_METRIC_PATH)
                    .getAttributeBuilder()
                        .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CLASS, WEIGHT)
                        .addRejectCheck(CapacityCheckerAndConverter.INSTANCE, CAPACITY)
                        .setValueConverter(CapacityCheckerAndConverter.INSTANCE, CAPACITY)
                        .end();
        dynamicLoadProvider.addChildResource(LOAD_METRIC_PATH)
                    .getAttributeBuilder()
                        .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, TYPE, WEIGHT, CAPACITY, PROPERTY)
                        .addRejectCheck(CapacityCheckerAndConverter.INSTANCE, CAPACITY)
                        .setValueConverter(CapacityCheckerAndConverter.INSTANCE, CAPACITY)
                        .addRejectCheck(PropertyCheckerAndConverter.INSTANCE, PROPERTY)
                        .setValueConverter(PropertyCheckerAndConverter.INSTANCE, PROPERTY)
                        .end();
        configurationBuilder.addChildResource(SSL_CONFIGURATION_PATH)
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CIPHER_SUITE, KEY_ALIAS, PROTOCOL)
                .end();

        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 2, 0));
    }

    private static void registerTransformers_1_3_0(SubsystemRegistration subsystem) {
        TransformationDescription.Tools.register(get1_3_0_1_4_0Description(), subsystem, ModelVersion.create(1, 3, 0));
    }

    private static void registerTransformers_1_4_0(SubsystemRegistration subsystem) {
        TransformationDescription.Tools.register(get1_3_0_1_4_0Description(), subsystem, ModelVersion.create(1, 4, 0));
    }

    /**
     * Transformation that discards/rejects session draining strategy.
     *
     * @return TransformationDescription
     */
    private static TransformationDescription get1_3_0_1_4_0Description() {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder configurationBuilder = builder.addChildResource(CONFIGURATION_PATH);

        configurationBuilder
                .getAttributeBuilder()
                .addRejectCheck(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                .setDiscard(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                .end();

        return builder.build();
    }

    /**
     * Converts doubles to ints, rejects expressions or double values that are not equivalent to integers.
     *
     * Package protected solely to allow unit testing.
     */
    static class CapacityCheckerAndConverter extends DefaultCheckersAndConverter {

        static final CapacityCheckerAndConverter INSTANCE = new CapacityCheckerAndConverter();

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ModClusterMessages.MESSAGES.capacityIsExpressionOrGreaterThanIntegerMaxValue(attributes.get(CAPACITY.getName()));
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (checkForExpression(attributeValue)
                    || (attributeValue.isDefined() && !isIntegerValue(attributeValue.asDouble()))) {
                return true;
            }
            Long converted = convert(attributeValue);
            return (converted != null && (converted > Integer.MAX_VALUE || converted < Integer.MIN_VALUE));
        }

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            Long converted = convert(attributeValue);
            if (converted != null && converted <= Integer.MAX_VALUE && converted >= Integer.MIN_VALUE) {
                attributeValue.set((int)converted.longValue());
            }
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            //Not used for discard
            return false;
        }

        private Long convert(ModelNode attributeValue) {
            if (attributeValue.isDefined() && !checkForExpression(attributeValue)) {
                double raw = attributeValue.asDouble();
                if (isIntegerValue(raw)) {
                    return Math.round(raw);
                }
            }
            return null;
        }

        private boolean isIntegerValue(double raw) {
            return raw == Double.valueOf(Math.round(raw)).doubleValue();
        }

    }

    private static class PropertyCheckerAndConverter extends DefaultCheckersAndConverter {
        private static final PropertyCheckerAndConverter INSTANCE = new PropertyCheckerAndConverter();
        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ModClusterMessages.MESSAGES.propertyCanOnlyHaveOneEntry();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
              if (attributeValue.isDefined()) {
                  if (attributeValue.asPropertyList().size() > 1) {
                      return true;
                  }
              }
              return false;
        }

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                List<Property> list = attributeValue.asPropertyList();
                if (list.size() == 1) {
                    attributeValue.set(list.get(0).getName(), list.get(0).getValue().asString());
                }
            }
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            //Not used for discard
            return false;
        }
    }

    private static class SessionDrainingStrategyChecker extends DefaultCheckersAndConverter {
        private static final SessionDrainingStrategyChecker INSTANCE = new SessionDrainingStrategyChecker();

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ModClusterMessages.MESSAGES.sessionDrainingStrategyMustBeUndefinedOrDefault();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                TransformationContext context) {
            if (attributeValue.isDefined()) {
                if (checkForExpression(attributeValue)) {
                    return true;
                }
                return !attributeValue.asString().equals("DEFAULT");
            }
            return false;
        }

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                TransformationContext context) {
            //No conversion needed
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue,
                TransformationContext context) {
            if (attributeValue.isDefined()) {
                if (checkForExpression(attributeValue)) {
                    return false;
                }
                return attributeValue.asString().equals("DEFAULT");
            }
            return false;
        }
    }
}
