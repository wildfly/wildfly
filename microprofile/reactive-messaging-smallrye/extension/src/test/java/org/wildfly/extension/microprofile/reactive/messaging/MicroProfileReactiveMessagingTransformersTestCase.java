/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not working yet")
public class MicroProfileReactiveMessagingTransformersTestCase extends AbstractSubsystemTest {

    public MicroProfileReactiveMessagingTransformersTestCase() {
        super(MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME, new MicroProfileReactiveMessagingExtension());
    }

    @Test
    public void testTransformers_1_0_0() throws Exception {
        ModelVersion modelVersion = MicroProfileReactiveMessagingExtension.VERSION_1_0_0;
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        builder.setSubsystemXml(readResource("microprofile-reactive-messaging-smallrye-1.0.xml"));
        LegacyKernelServicesInitializer legacyInitializer =
                builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_XP_5, modelVersion)
                .addMavenResourceURL("org.jboss.eap.xp:wildfly-microprofile-reactive-messaging:" + ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
//                .addParentFirstClassPattern("org.jboss.msc.*")
//                .addParentFirstClassPattern("org.jboss.msc.service.*")
                .dontPersistXml();
        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                if (modelNode.isDefined() && modelNode.getType() == ModelType.OBJECT && modelNode.keys().size() == 0) {
                    return new ModelNode();
                }
                return modelNode;
            }
        }, true);
        ModelNode transformed = mainServices.readTransformedModel(modelVersion);
        Assert.assertTrue(transformed.isDefined());
    }

    private void testTransformers(ModelVersion modelVersion) throws Exception {
    }

    @Test
    public void testRejectingTransformers_1_0_0() throws Exception {

    }
}
