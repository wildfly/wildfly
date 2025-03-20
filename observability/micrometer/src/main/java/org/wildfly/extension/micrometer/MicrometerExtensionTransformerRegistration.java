/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_1_1_0;
import static org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar.ENDPOINT;
import static org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar.STEP;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;

@MetaInfServices
public class MicrometerExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicrometerConfigurationConstants.NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        final ModelVersion currentModel = registration.getCurrentSubsystemVersion();
        ChainedTransformationDescriptionBuilder chainedBuilder =
            TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        // 2.0.0_Community (WildFly 36) to 1.1.0 (WildFly 33)
        from2(chainedBuilder.createBuilder(currentModel, VERSION_1_1_0.getVersion()));

        chainedBuilder.buildAndRegister(registration, new ModelVersion[]{VERSION_1_1_0.getVersion()});
    }

    private void from2(ResourceTransformationDescriptionBuilder builder) {
        builder.rejectChildResource(PrometheusRegistryDefinitionRegistrar.PATH);

        ResourceTransformationDescriptionBuilder otlp = builder.addChildResource(OtlpRegistryDefinitionRegistrar.PATH);
        otlp.addOperationTransformationOverride(ADD).setCustomOperationTransformer(OtlpOperationTransformer.INSTANCE);
        otlp.addOperationTransformationOverride(REMOVE).setCustomOperationTransformer(OtlpOperationTransformer.INSTANCE);
        otlp.addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION)
            .setCustomOperationTransformer(OtlpOperationTransformer.INSTANCE);
        otlp.addOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION)
            .setCustomOperationTransformer(OtlpOperationTransformer.INSTANCE);

        builder.setCustomResourceTransformer(new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) {
                if (resource.hasChild(OtlpRegistryDefinitionRegistrar.PATH)) {
                    Resource otlp = resource.removeChild(OtlpRegistryDefinitionRegistrar.PATH);
                    resource.getModel().get(STEP.getName()).set(otlp.getModel().get(STEP.getName()));
                    resource.getModel().get(ENDPOINT.getName()).set(otlp.getModel().get(ENDPOINT.getName()));
                }
                context.addTransformedResourceFromRoot(address, resource);
            }
        });

    }

    private static class OtlpOperationTransformer implements OperationTransformer {
        static final OtlpOperationTransformer INSTANCE = new OtlpOperationTransformer();
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            String name = operation.get(OP).asString();

            List<ModelNode> operations = new ArrayList<>();
            if (name.equals(ADD) || name.equals(REMOVE)) {
                Long step = operation.hasDefined(STEP.getName()) ? operation.get(STEP.getName()).resolve().asLong() : null;
                String endpoint = operation.hasDefined(ENDPOINT.getName()) ? operation.get(ENDPOINT.getName()).resolve().asString() : null;
                if (step == null) {
                    operations.add(Util.getUndefineAttributeOperation(address.getParent(), STEP.getName()));
                } else {
                    operations.add(Util.getWriteAttributeOperation(address.getParent(), STEP.getName(), new ModelNode(step)));
                }

                if (endpoint == null) {
                    operations.add(Util.getUndefineAttributeOperation(address.getParent(), ENDPOINT.getName()));
                } else {
                    operations.add(Util.getWriteAttributeOperation(address.getParent(), ENDPOINT.getName(), new ModelNode(endpoint)));
                }
            } else if (name.equals(WRITE_ATTRIBUTE_OPERATION) || name.equals(UNDEFINE_ATTRIBUTE_OPERATION)) {
                PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
                ModelNode replaced = operation.clone();
                operation.get(OP_ADDR).set(addr.getParent().toModelNode());
                operations.add(replaced);
            }

            ModelNode transformed;
            if (operations.size() == 1) {
                transformed = operations.get(0);
            } else {
                transformed = Util.createCompositeOperation(operations);
            }

            return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }
}
