/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.ANNOTATIONS;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.GLOBAL_MODULES;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.META_INF;
import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.SERVICES;
import static org.jboss.as.model.test.FailedOperationTransformationConfig.REJECTED_RESOURCE;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Eduardo Martins
 */
public class EeSubsystemTestCase extends AbstractSubsystemBaseTest {

    public EeSubsystemTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }


    @Test
    public void testTransformers712() throws Exception {
        //Due to https://issues.jboss.org/browse/AS7-4892 the jboss-descriptor-property-replacement
        //does not get set properly on 7.1.2, so let's do a reject test.
        testTransformers712(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersEAP600() throws Exception {
        //Due to https://issues.jboss.org/browse/AS7-4892 the jboss-descriptor-property-replacement
        //does not get set properly on 7.1.2, so let's do a reject test.
        testTransformers712(ModelTestControllerVersion.EAP_6_0_0);
    }

    private void testTransformers712(ModelTestControllerVersion controllerVersion) throws Exception {
        //Due to https://issues.jboss.org/browse/AS7-4892 the jboss-descriptor-property-replacement
        //does not get set properly on 7.1.2, so let's do a reject test

        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);

        try {
            //Override the core model version to make sure that our custom transformer for model version 1.0.0 running on 7.1.2 kicks in
            System.setProperty("jboss.test.core.model.version.override", "1.2.0");

            KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

            // Add legacy subsystems
            builder.createLegacyKernelServicesBuilder(null, ModelTestControllerVersion.V7_1_2_FINAL, modelVersion)
                    .addMavenResourceURL("org.jboss.as:jboss-as-ee:7.1.2.Final");

            KernelServices mainServices = builder.build();
            KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
            Assert.assertTrue(mainServices.isSuccessfulBoot());
            Assert.assertTrue(legacyServices.isSuccessfulBoot());

            List<ModelNode> bootOps = builder.parseXmlResource("subsystem-transformers.xml");
            ModelTestUtils.checkFailedTransformedBootOperations(
                    mainServices,
                    modelVersion,
                    bootOps,
                    new FailedOperationTransformationConfig()
                            .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM), new JBossDescriptorPropertyReplacementConfig()));

            checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
                @Override
                public ModelNode fixModel(ModelNode modelNode) {
                    Assert.assertTrue(modelNode.get(EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT).asBoolean());
                    //Replace the value used in the xml
                    modelNode.get(EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT).set(new ValueExpression("${test-exp2:false}"));
                    return modelNode;
                }
            });
        } finally {
            System.clearProperty("jboss.test.core.model.version.override");
        }
    }

    @Test
    public void testTransformers713() throws Exception {
        testTransformers1_0_0_post712(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformers720() throws Exception {
        testTransformers1_0_0_post712(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformersEAP601() throws Exception {
        testTransformers1_0_0_post712(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        testTransformers1_0_0_post712(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        testTransformers1_0_0_post712(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformers1_0_0_post712(ModelTestControllerVersion controllerVersion) throws Exception {
        //Do a normal transformation test containing parts of the subsystem that work everywhere
        String subsystemXml = readResource("subsystem-transformers.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testTransformers713Reject() throws Exception {
        testTransformers1_0_0_reject(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformers720Reject() throws Exception {
        testTransformers1_0_0_reject(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformersEAP601Reject() throws Exception {
        testTransformers1_0_0_reject(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformersEAP610Reject() throws Exception {
        testTransformers1_0_0_reject(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformersEAP611Reject() throws Exception {
        testTransformers1_0_0_reject(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformers1_0_0_reject(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        List<ModelNode> xmlOps = builder.parseXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        FailedOperationTransformationConfig.ChainedConfig chained =
                FailedOperationTransformationConfig.ChainedConfig.createBuilder(GlobalModulesDefinition.INSTANCE.getName(), EESubsystemModel.ANNOTATION_PROPERTY_REPLACEMENT)
                .addConfig(new GlobalModulesConfig())
                .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(EESubsystemModel.ANNOTATION_PROPERTY_REPLACEMENT) {
                    @Override
                    protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
                        ModelNode resolved = super.correctValue(toResolve, isWriteAttribute);
                        //Make it a boolean
                        return new ModelNode(resolved.asBoolean());
                    }})
                .build();


        FailedOperationTransformationConfig config =  new FailedOperationTransformationConfig()
        .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM), chained)
        .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.CONTEXT_SERVICE)), REJECTED_RESOURCE)
        .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY)), REJECTED_RESOURCE)
        .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE)), REJECTED_RESOURCE)
        .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE)), REJECTED_RESOURCE);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);
    }

    @Test
    public void testTransformersDiscardsImpliedValues713() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformersDiscardsImpliedValues720() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformersDiscardsImpliedValuesEAP601() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformersDiscardsImpliedValuesEAP610() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformersDiscardsImpliedValuesEAP611() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("subsystem-transformers-discard.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, new ModelFixer() {
                    // The regular model will have the new attributes because they are in the xml,
                    // but the reverse controller model will not because transformation strips them
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        for(ModelNode node : modelNode.get(GLOBAL_MODULES).asList()) {
                            if ("org.apache.log4j".equals(node.get(NAME).asString())) {
                                if (!node.has(ANNOTATIONS)) {
                                    node.get(ANNOTATIONS).set(false);
                                }
                                if (!node.has(META_INF)) {
                                    node.get(META_INF).set(false);
                                }
                                if (!node.has(SERVICES)) {
                                    node.get(SERVICES).set(true);
                                }
                            }
                        }

                        return modelNode;
                    }
                });

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode globalModules = mainServices.readTransformedModel(modelVersion).get(ModelDescriptionConstants.SUBSYSTEM, "ee").get(GlobalModulesDefinition.GLOBAL_MODULES);
        for(ModelNode node : globalModules.asList()) {
            if(node.hasDefined(ANNOTATIONS) ||
                    node.hasDefined(SERVICES) ||
                    node.hasDefined(META_INF)) {
                Assert.fail(node + " -- attributes not discarded");
            }
        }
    }

    private static final class JBossDescriptorPropertyReplacementConfig extends AttributesPathAddressConfig<JBossDescriptorPropertyReplacementConfig> {

        public JBossDescriptorPropertyReplacementConfig() {
            super(EESubsystemModel.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.asString().equals("true");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(true);
        }
    }

    private static final class GlobalModulesConfig extends AttributesPathAddressConfig<GlobalModulesConfig> {

        public GlobalModulesConfig() {
            super(GlobalModulesDefinition.INSTANCE.getName());
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            for (ModelNode module : attribute.asList()) {
                if (module.has(ANNOTATIONS) || module.has(META_INF) || module.has(SERVICES)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            for (ModelNode module : toResolve.asList()) {
                module.remove(ANNOTATIONS);
                module.remove(META_INF);
                module.remove(SERVICES);
            }
            return toResolve;
        }
    }


}
