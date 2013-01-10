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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
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
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.jboss.as.modcluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.jboss.as.modcluster.ModClusterSSLResourceDefinition.PROTOCOL;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
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

    public static final String SUBSYSTEM_NAME = "modcluster";
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 3;
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


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debugf("Activating Mod_cluster Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ModClusterDefinition(context.isRuntimeOnlyRegistrationValid()));

        final ManagementResourceRegistration configuration = registration.registerSubModel(new ModClusterConfigResourceDefinition());

        configuration.registerSubModel(new ModClusterSSLResourceDefinition());
        final ManagementResourceRegistration dynamicLoadProvider = configuration.registerSubModel(DynamicLoadProviderDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(LoadMetricDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(CustomLoadMetricDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(new ModClusterSubsystemXMLWriter());

        registerTransformers_1_2_0(subsystem);
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

        TransformersSubRegistration transformers = subsystem.registerModelTransformers(ModelVersion.create(1, 2, 0), ResourceTransformer.DEFAULT);

        // ModClusterConfigResourceDefinition
        RejectExpressionValuesTransformer configRejectExpressionTransformer = new RejectExpressionValuesTransformer(ADVERTISE, AUTO_ENABLE_CONTEXTS, FLUSH_PACKETS, STICKY_SESSION, STICKY_SESSION_REMOVE, STICKY_SESSION_FORCE, PING);
        TransformersSubRegistration config = transformers.registerSubResource(CONFIGURATION_PATH, (ResourceTransformer) configRejectExpressionTransformer);
        config.registerOperationTransformer(ModelDescriptionConstants.ADD, configRejectExpressionTransformer);


        // ModClusterSSLResourceDefinition
        RejectExpressionValuesTransformer sslRejectExpressionTransformer = new RejectExpressionValuesTransformer(CIPHER_SUITE, KEY_ALIAS, PROTOCOL);
        TransformersSubRegistration ssl = transformers.registerSubResource(SSL_CONFIGURATION_PATH, (ResourceTransformer) sslRejectExpressionTransformer);
        ssl.registerOperationTransformer(ADD, sslRejectExpressionTransformer);
        ssl.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, sslRejectExpressionTransformer.getWriteAttributeTransformer());

        // DynamicLoadProviderDefinition
        RejectExpressionValuesTransformer dynamicProviderRejectExpressionTransformer = new RejectExpressionValuesTransformer(DECAY, HISTORY);
        TransformersSubRegistration dynamicProvider = config.registerSubResource(DYNAMIC_LOAD_PROVIDER_PATH, (ResourceTransformer) dynamicProviderRejectExpressionTransformer);
        dynamicProvider.registerOperationTransformer(ADD, dynamicProviderRejectExpressionTransformer);
        dynamicProvider.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, dynamicProviderRejectExpressionTransformer.getWriteAttributeTransformer());

        // CustomLoadMetricDefinition
        RejectExpressionValuesTransformer customRejectExpressionTransformer = new RejectExpressionValuesTransformer(CLASS);
        TransformersSubRegistration customMetric = dynamicProvider.registerSubResource(CUSTOM_LOAD_METRIC_PATH, (ResourceTransformer) customRejectExpressionTransformer);
        customMetric.registerOperationTransformer(ADD, customRejectExpressionTransformer);
        customMetric.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, customRejectExpressionTransformer.getWriteAttributeTransformer());

        // LoadMetricDefinition
        RejectExpressionValuesTransformer loadMetricRejectExpressionTransformer = new RejectExpressionValuesTransformer(TYPE, WEIGHT, CAPACITY, PROPERTY);
        ChainedResourceTransformer loadMetricResourceTransformer = new ChainedResourceTransformer(loadMetricRejectExpressionTransformer.getChainedTransformer(), ConvertCapacityTransformer.INSTANCE);
        TransformersSubRegistration metric = dynamicProvider.registerSubResource(LOAD_METRIC_PATH, loadMetricResourceTransformer);
        metric.registerOperationTransformer(ADD, new ChainedOperationTransformer(loadMetricRejectExpressionTransformer, ConvertCapacityTransformer.INSTANCE));
        metric.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(loadMetricRejectExpressionTransformer.getWriteAttributeTransformer(), ConvertCapacityTransformer.INSTANCE.getWriteAttributeTransformer()));

    }

    private static class ConvertCapacityTransformer implements ChainedResourceTransformerEntry, OperationTransformer {
        private static final ConvertCapacityTransformer INSTANCE = new ConvertCapacityTransformer();

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                throws OperationFailedException {
            return internalTransformOperation(operation, CAPACITY.getName());
        }

        @Override
        public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
                throws OperationFailedException {
            resource.writeModel(internalTransform(resource.getModel(), CAPACITY.getName()));
        }

        private ModelNode fixProperties(ModelNode model) {
            ModelNode fixed = model.clone();
            ModelNode modelProps = model.get(LoadMetricDefinition.PROPERTY.getName());
            if (modelProps.isDefined()) {
                for (Property p : modelProps.asPropertyList()) {//legacy was broken, only one property can be passed trough
                    fixed.get(LoadMetricDefinition.PROPERTY.getName()).set(p.getName(), p.getValue().asString());
                }
            }
            return fixed;
        }

        private TransformedOperation internalTransformOperation(ModelNode operation, String name) {
            return new TransformedOperation(internalTransform(operation, name), OperationResultTransformer.ORIGINAL_RESULT);
        }

        private ModelNode internalTransform(ModelNode model, String name) {
            model = model.clone();
            if (model.hasDefined(name) && model.get(name).getType() != ModelType.EXPRESSION) {
                model.get(name).set(Math.round(model.get(name).asDouble()));
            }
            return fixProperties(model);
        }

        OperationTransformer getWriteAttributeTransformer() {
            return new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                        throws OperationFailedException {
                    return internalTransformOperation(operation, VALUE);
                }
            };
        }
    }
}
