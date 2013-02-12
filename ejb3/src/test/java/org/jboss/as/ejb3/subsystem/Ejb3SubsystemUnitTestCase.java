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

package org.jboss.as.ejb3.subsystem;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.threads.PoolAttributeDefinitions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class Ejb3SubsystemUnitTestCase extends AbstractSubsystemBaseTest {

    public Ejb3SubsystemUnitTestCase() {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer_1_1_0("7.1.2.Final", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer_1_1_0("7.1.3.Final", ModelTestControllerVersion.V7_1_3_FINAL);
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer_1_1_0(String mavenVersion, ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = "transform_1_1_0.xml";   //This has no expressions not understood by 1.1.0
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-ejb3:" + mavenVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-threads:" + mavenVersion);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        checkSubsystemModelTransformation(mainServices, modelVersion, V_1_1_0_FIXER);
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions_1_1_0("7.1.2.Final", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions_1_1_0("7.1.3.Final", ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testRejectExpressions_1_1_0(String mavenVersion, ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-ejb3:" + mavenVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-threads:" + mavenVersion);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, xmlOps, getConfig());
    }

    private FailedOperationTransformationConfig getConfig() {
        PathAddress subsystemAddress = PathAddress.pathAddress(EJB3Extension.SUBSYSTEM_PATH);
        FailedOperationTransformationConfig.RejectExpressionsConfig keepaliveOnly =
                new FailedOperationTransformationConfig.RejectExpressionsConfig(PoolAttributeDefinitions.KEEPALIVE_TIME);

        return new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress,
                        FailedOperationTransformationConfig.ChainedConfig.createBuilder(
                                EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS,
                                EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN)
                                .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS))
                                .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN)).build())
                .addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.THREAD_POOL)),
                        keepaliveOnly)
                .addFailedAttribute(subsystemAddress.append(StrictMaxPoolResourceDefinition.INSTANCE.getPathElement()),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT))
                .addFailedAttribute(subsystemAddress.append(FilePassivationStoreResourceDefinition.INSTANCE.getPathElement()),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(FilePassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT))
                .addFailedAttribute(subsystemAddress.append(ClusterPassivationStoreResourceDefinition.INSTANCE.getPathElement()),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(ClusterPassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT))
                .addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(TimerServiceResourceDefinition.PATH))
                .addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH, ChannelCreationOptionResource.INSTANCE.getPathElement()),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(ChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE));
    }

    private static final ModelFixer V_1_1_0_FIXER = new ModelFixer()  {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // Legacy parser stores incorrect type
            ModelNode iiopEnabled = modelNode.get("service", "iiop", "enable-by-default");
            if (iiopEnabled.getType() == ModelType.STRING) {
                iiopEnabled.set(iiopEnabled.asBoolean());
            }
            ModelNode useQualfified = modelNode.get("service", "iiop", "use-qualified-name");
            if (useQualfified.getType() == ModelType.STRING) {
                useQualfified.set(useQualfified.asBoolean());
            }
            // Bogus 'name' attributes that weren't in the legacy resource definition.
            // We don't include them in transformed resources either; if the server wants
            // them at runtime, the bogus server code will add them anyway
            modelNode.get("file-passivation-store", "file").remove("name");
            modelNode.get("cluster-passivation-store", "cluster").remove("name");
            modelNode.get("strict-max-bean-instance-pool", "slsb-strict-max-pool").remove("name");
            modelNode.get("strict-max-bean-instance-pool", "mdb-strict-max-pool").remove("name");
            modelNode.get("cache", "simple").remove("name");
            modelNode.get("cache", "passivating").remove("name");
            modelNode.get("cache", "clustered").remove("name");
            return modelNode;
        }
    };
}
