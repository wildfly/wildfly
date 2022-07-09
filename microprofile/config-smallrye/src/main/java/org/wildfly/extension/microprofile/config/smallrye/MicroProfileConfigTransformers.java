/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.SIMPLE_EXPRESSIONS;

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

        registerTransformers_WildFly_20(builder.createBuilder(MicroProfileConfigExtension.VERSION_1_1_0, MicroProfileConfigExtension.VERSION_1_0_0));
        registerTransformers_WildFly_26(builder.createBuilder(MicroProfileConfigExtension.VERSION_2_0_0, MicroProfileConfigExtension.VERSION_1_1_0));

        builder.buildAndRegister(registration, new ModelVersion[] { MicroProfileConfigExtension.VERSION_1_1_0, MicroProfileConfigExtension.VERSION_1_0_0});
    }

    private void registerTransformers_WildFly_20(ResourceTransformationDescriptionBuilder builder) {
        // reject the ordinal attribute onf a config-source if it holds an expression
       builder.addChildResource(MicroProfileConfigExtension.CONFIG_SOURCE_PATH).getAttributeBuilder()
                .addRejectCheck(SIMPLE_EXPRESSIONS, ConfigSourceDefinition.ORDINAL);
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
