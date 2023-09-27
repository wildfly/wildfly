/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class MicroProfileConfigTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicroProfileConfigExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerTransformers_WildFly_26(builder.createBuilder(MicroProfileConfigExtension.VERSION_2_0_0, MicroProfileConfigExtension.VERSION_1_1_0));

        builder.buildAndRegister(registration, new ModelVersion[] { MicroProfileConfigExtension.VERSION_1_1_0});
    }

    private void registerTransformers_WildFly_26(ResourceTransformationDescriptionBuilder builder) {
        Map<String, RejectAttributeChecker> checkers = new HashMap<>();
        checkers.put(ConfigSourceDefinition.ROOT.getName(), new RejectAttributeChecker.SimpleAcceptAttributeChecker(ModelNode.FALSE) {
            @Override
            protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                if (!attributeValue.hasDefined(ConfigSourceDefinition.ROOT.getName())) {
                    return false;
                }
                return super.rejectAttribute(address, attributeName, attributeValue, context);
            }
        });

        builder.addChildResource(MicroProfileConfigExtension.CONFIG_SOURCE_PATH).getAttributeBuilder()
                .addRejectCheck(new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(checkers), ConfigSourceDefinition.DIR)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        if (attributeValue.hasDefined(ConfigSourceDefinition.ROOT.getName())) {
                            attributeValue.remove(ConfigSourceDefinition.ROOT.getName());
                        }
                    }
                }, ConfigSourceDefinition.DIR);
    }
}
