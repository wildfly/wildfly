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
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
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

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-ee_4_0.xsd";
    }

    @Test
    public void testTransformersEAP620() throws Exception {
        testTransformers1_1(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1,0));
    }

    @Test
    public void testTransformersEAP630() throws Exception {
        testTransformers1_1(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1,1));
    }

    @Test
    public void testTransformersEAP640() throws Exception {
        testTransformers1_1(ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1,1));
    }

    private void testTransformers1_1(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        //Do a normal transformation test containing parts of the subsystem that work everywhere
        String subsystemXml = readResource("subsystem-transformers.xml");
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
    public void testTransformersEAP620Reject() throws Exception {
        testTransformers1_0_x_reject(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1,0));
    }

    @Test
    public void testTransformersEAP630Reject() throws Exception {
        testTransformers1_1_x_reject(ModelTestControllerVersion.EAP_6_3_0);
    }

    @Test
    public void testTransformersEAP640Reject() throws Exception {
        testTransformers1_1_x_reject(ModelTestControllerVersion.EAP_6_4_0);
    }

    private void testTransformers1_0_x_reject(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = readResource("subsystem.xml");
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

    private void testTransformers1_1_x_reject(ModelTestControllerVersion controllerVersion) throws Exception {
            String subsystemXml = readResource("subsystem.xml");
            //Use the non-runtime version of the extension which will happen on the HC
            KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

            List<ModelNode> xmlOps = builder.parseXml(subsystemXml);

           ModelVersion modelVersion = ModelVersion.create(1,1);
            // Add legacy subsystems
            builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                    .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion());

            KernelServices mainServices = builder.build();
            Assert.assertTrue(mainServices.isSuccessfulBoot());

            FailedOperationTransformationConfig config =  new FailedOperationTransformationConfig()
            .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.CONTEXT_SERVICE)), REJECTED_RESOURCE)
            .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_THREAD_FACTORY)), REJECTED_RESOURCE)
            .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE)), REJECTED_RESOURCE)
            .addFailedAttribute(PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM, PathElement.pathElement(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE)), REJECTED_RESOURCE);

            ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);
        }


    @Test
    public void testTransformersDiscardsImpliedValuesEAP620() throws Exception {
        testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    public void testTransformersDiscardsImpliedValuesEAP630() throws Exception {
        testTransformersDiscardsImpliedValues1_1_0(ModelTestControllerVersion.EAP_6_3_0);
    }

    @Test
    public void testTransformersDiscardsImpliedValuesEAP640() throws Exception {
        testTransformersDiscardsImpliedValues1_1_0(ModelTestControllerVersion.EAP_6_4_0);
    }

    private void testTransformersDiscardsImpliedValues1_0_0(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("subsystem-transformers-discard.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        // The regular model will have the new attributes because they are in the xml,
        // but the reverse controller model will not because transformation strips them
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, modelNode -> {
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

    private void testTransformersDiscardsImpliedValues1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("subsystem-transformers-discard.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ee:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());


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
