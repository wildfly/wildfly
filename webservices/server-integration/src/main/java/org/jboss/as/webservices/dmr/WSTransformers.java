/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.dmr.WSExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.webservices.dmr.WSExtension.MODEL_VERSION_1_2;
import static org.jboss.as.webservices.dmr.WSExtension.MODEL_VERSION_2_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WSTransformers implements ExtensionTransformerRegistration{

    @Override
    public String getSubsystemName() {
        return WSExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);
        ResourceTransformationDescriptionBuilder builder_2_0 = chainedBuilder.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_2_0);
        builder_2_0.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attributes.STATISTICS_ENABLED);

        ResourceTransformationDescriptionBuilder builder_1_2 = chainedBuilder.createBuilder(MODEL_VERSION_2_0, MODEL_VERSION_1_2);
        builder_1_2.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.STATISTICS_ENABLED, Attributes.WSDL_URI_SCHEME, Attributes.WSDL_PATH_REWRITE_RULE);

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                MODEL_VERSION_1_2,
                MODEL_VERSION_2_0,
        });
    }
}
