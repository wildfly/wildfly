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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.CustomLoadMetricDefinition.CLASS;
import static org.wildfly.extension.mod_cluster.DynamicLoadProviderDefinition.DECAY;
import static org.wildfly.extension.mod_cluster.DynamicLoadProviderDefinition.HISTORY;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.CAPACITY;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.PROPERTY;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.TYPE;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.WEIGHT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.AUTO_ENABLE_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_PACKETS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PING;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PROTOCOL;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
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
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    static final String LEGACY_SUBSYSTEM_NAME = "modcluster";
    // Might it be possible to rename this subsystem?!
    public static final String SUBSYSTEM_NAME = LEGACY_SUBSYSTEM_NAME; //"mod_cluster";
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);
    static final PathElement LEGACY_SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ModClusterExtension.LEGACY_SUBSYSTEM_NAME);
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


    /**
     * {@inheritDoc}
     */
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

    private static void registerTransformers_1_2_0(SubsystemRegistration subsystem) {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder configurationBuilder = builder.addChildResource(CONFIGURATION_PATH);
        ResourceTransformationDescriptionBuilder dynamicLoadProvider = configurationBuilder
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ADVERTISE, AUTO_ENABLE_CONTEXTS, FLUSH_PACKETS, STICKY_SESSION, STICKY_SESSION_REMOVE, STICKY_SESSION_FORCE, PING)
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

    /**
     * Converts doubles to ints, rejects expressions or double values that are not equivalent to integers.
     *
     * Package protected solely to allow unit testing.
     */
    static class CapacityCheckerAndConverter extends DefaultCheckersAndConverter {

        static final CapacityCheckerAndConverter INSTANCE = new CapacityCheckerAndConverter();
        private static final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");

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

        private boolean checkForExpression(final ModelNode node) {
            return (node.getType() == ModelType.EXPRESSION || node.getType() == ModelType.STRING)
                    && EXPRESSION_PATTERN.matcher(node.asString()).matches();
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
}
