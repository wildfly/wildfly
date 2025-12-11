/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import static org.wildfly.iiop.openjdk.IIOPExtension.VERSION_3_0;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * IIOP Transformers used to transform current model version to legacy model versions for domain mode.
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class IIOPTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return IIOPExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        // register the transformations required for each legacy version after 3.0.0
        registerTransformers_3_0_0(chainedBuilder.createBuilder(currentModel, VERSION_3_0));

        // create the chained builder which incorporates all transformations
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                VERSION_3_0 });
    }

    /*
     * Transformers for changes in model version 4.0.0
     */
    private static void registerTransformers_3_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        subsystemBuilder.getAttributeBuilder()
                // require that both xxx-ssl-context attributes are defined or both are undefined
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return IIOPLogger.ROOT_LOGGER.inconsistentSSLContextDefinition();
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                      TransformationContext context) {
                        // Note: we don't limit checking the other attribute to the case where attributeValue
                        // is undefined. That would be sufficient for an add op transformation or a resource
                        // transformation, where this method would be called for both attributes. But we want
                        // to catch cases where both *were* undefined but an operation is defining just one.
                        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                        String otherAttribute = attributeName.equals(IIOPRootDefinition.CLIENT_SSL_CONTEXT.getName())
                                ? IIOPRootDefinition.SERVER_SSL_CONTEXT.getName()
                                : IIOPRootDefinition.CLIENT_SSL_CONTEXT.getName();
                        return model.hasDefined(attributeName) != model.hasDefined(otherAttribute);
                    }
                }, IIOPRootDefinition.CLIENT_SSL_CONTEXT, IIOPRootDefinition.SERVER_SSL_CONTEXT);
    }
}

