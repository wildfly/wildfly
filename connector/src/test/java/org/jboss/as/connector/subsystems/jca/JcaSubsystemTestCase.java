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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.threads.PoolAttributeDefinitions;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class JcaSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JcaSubsystemTestCase() {
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jca.xml");
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("jca-full.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("jca-full-expression.xml", "jca-full.xml");
    }


    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer(ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerAS72() throws Exception {
        testTransformer(ModelTestControllerVersion.V7_2_0_FINAL, ModelVersion.create(1, 2, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerAS72WithExpressions() throws Exception {
        testTransformer(ModelTestControllerVersion.V7_2_0_FINAL, ModelVersion.create(1, 2, 0), "jca-full-expression.xml");
    }

    @Test
    public void testTransformerWF8() throws Exception {
        testTransformerWF(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL, ModelVersion.create(2, 0, 0), "jca-full.xml");
    }

    @Test
    public void testTransformerWF8WithExpressions() throws Exception {
        testTransformerWF(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL, ModelVersion.create(2, 0, 0), "jca-full-expression.xml");
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.as:jboss-as-threads:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource(xmlResourceName);


        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER, PathElement.pathElement(WORKMANAGER_SHORT_RUNNING)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformerWF(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, String xmlResourceName) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(xmlResourceName);

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.wildfly:wildfly-threads:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class))
                //.skipReverseControllerCheck();
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //These two are true in the original model but get removed by the transformers, so they default to false. Set them to true
                //modelNode.get(Constants.TRACER, Constants.TRACER). add(new ModelNode(Constants.TRACER));
                //.add(Constants.TRACER);
                modelNode.get(Constants.TRACER, Constants.TRACER, TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getName()).set(true);
                return modelNode;

            }
        });

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testRejectExpressions1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.as:jboss-as-threads:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("jca-full-expression.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, xmlOps,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaWorkManagerDefinition.PATH_WORK_MANAGER,
                                PathElement.pathElement(WORKMANAGER_SHORT_RUNNING)),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT, PoolAttributeDefinitions.KEEPALIVE_TIME))
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaWorkManagerDefinition.PATH_WORK_MANAGER,
                                PathElement.pathElement(WORKMANAGER_LONG_RUNNING)),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT, PoolAttributeDefinitions.KEEPALIVE_TIME))
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(JcaSubsystemRootDefinition.PATH_SUBSYSTEM, JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER, PathElement.pathElement(WORKMANAGER_SHORT_RUNNING)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }


}
